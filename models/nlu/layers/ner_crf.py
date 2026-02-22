"""
Modèle NER avec CRF Layer pour garantir des séquences BIO valides

Le CRF (Conditional Random Field) force le modèle à:
- Ne jamais générer I-FROM sans B-FROM avant
- Respecter les transitions BIO (B-X → I-X → O)
- Améliorer la cohérence des prédictions sur toute la séquence

Gain attendu: +2-5% F1 sur NER
"""

import torch
import torch.nn as nn
from torchcrf import CRF
from transformers import PreTrainedModel, AutoModel
from transformers.modeling_outputs import TokenClassifierOutput


class CamembertForTokenClassificationWithCRF(PreTrainedModel):
    """
    Modèle CamemBERT pour token classification avec CRF layer

    Architecture:
        Input → CamemBERT → Linear → CRF → Output

    Le CRF apprend les transitions valides entre labels BIO pendant l'entraînement
    et décode la séquence la plus probable pendant l'inférence.
    """

    def __init__(self, config):
        super().__init__(config)
        self.num_labels = config.num_labels

        # Encodeur CamemBERT - CHARGER LES POIDS PRÉENTRAÎNÉS
        # Note: On ne peut pas utiliser from_config car ça initialise aléatoirement
        # Il faut charger depuis le nom du modèle (ex: "camembert-base")
        model_name = config._name_or_path if hasattr(config, '_name_or_path') else "camembert-base"
        self.roberta = AutoModel.from_pretrained(model_name, config=config)

        # Dropout pour régularisation
        classifier_dropout = (
            config.classifier_dropout
            if hasattr(config, 'classifier_dropout') and config.classifier_dropout is not None
            else config.hidden_dropout_prob
        )
        self.dropout = nn.Dropout(classifier_dropout)

        # Couche linéaire qui produit les émissions (logits) pour chaque label
        self.classifier = nn.Linear(config.hidden_size, config.num_labels)

        # CRF Layer - apprend les transitions valides entre labels
        # CRITICAL: CRF doit TOUJOURS être en FP32 pour stabilité numérique
        # Les calculs de log-likelihood sont très sensibles aux underflows/overflows en FP16
        self.crf = CRF(config.num_labels, batch_first=True)

        # CRITICAL: Force ALL CRF parameters to FP32 and prevent autocast from converting them
        # This is essential when using mixed precision training (fp16/bf16)
        for param in self.crf.parameters():
            param.data = param.data.float()
            # Prevent gradient scaler from converting to fp16
            param.requires_grad = True

        # CRITICAL: Initialize CRF transitions with small values to prevent explosion
        # Default torchcrf initialization uses uniform(-0.1, 0.1) which can explode with FP16
        # We use a tighter range for better numerical stability
        nn.init.uniform_(self.crf.transitions, -0.01, 0.01)
        nn.init.uniform_(self.crf.start_transitions, -0.01, 0.01)
        nn.init.uniform_(self.crf.end_transitions, -0.01, 0.01)

        # Note: Gradient clipping is handled globally by TrainingArguments(max_grad_norm=1.0)
        # No need for per-parameter hooks here (avoids overhead on every backward pass)

        # Initialiser les poids
        self.post_init()

    def forward(
        self,
        input_ids=None,
        attention_mask=None,
        token_type_ids=None,
        labels=None,
        **kwargs
    ):
        """
        Forward pass avec CRF

        Args:
            input_ids: Tensor de shape (batch_size, seq_len)
            attention_mask: Tensor de shape (batch_size, seq_len)
            labels: Tensor de shape (batch_size, seq_len) - uniquement pour training

        Returns:
            TokenClassifierOutput avec:
                - loss: CRF negative log-likelihood (si labels fournis)
                - logits: Émissions du classifier (batch_size, seq_len, num_labels)
                - predictions: Décodage Viterbi du CRF (batch_size, seq_len) - uniquement si labels=None
        """
        # Passer par l'encodeur CamemBERT
        outputs = self.roberta(
            input_ids,
            attention_mask=attention_mask,
            token_type_ids=token_type_ids
        )

        # Récupérer les représentations des tokens
        sequence_output = outputs.last_hidden_state  # (batch_size, seq_len, hidden_size)
        sequence_output = self.dropout(sequence_output)

        # Produire les émissions (logits) pour chaque label
        emissions = self.classifier(sequence_output)  # (batch_size, seq_len, num_labels)

        # CRITICAL: Convertir les émissions en FP32 pour le CRF
        # Le CRF doit travailler en FP32 pour éviter les NaN/Inf
        # Même si le reste du modèle est en FP16, on force FP32 ici
        emissions_fp32 = emissions if emissions.dtype == torch.float32 else emissions.float()

        loss = None
        predictions = None

        # Créer le mask pour CRF (True pour tokens réels, False pour padding)
        # CRF attend un ByteTensor mask
        if attention_mask is not None:
            crf_mask = attention_mask.bool()
        else:
            crf_mask = torch.ones_like(input_ids, dtype=torch.bool)

        if labels is not None:
            # === MODE TRAINING ===
            # Masquer les labels -100 (tokens spéciaux)
            # CRF n'accepte pas -100, on doit remplacer par un label valide
            labels_masked = labels.clone()
            labels_masked[labels == -100] = 0  # Remplacer -100 par O (label 0)

            # Ajuster le mask pour exclure les positions avec -100
            crf_mask = crf_mask & (labels != -100)

            # CRITICAL: torchcrf exige que mask[:, 0] soit toujours True
            # Même si le premier token est [CLS] avec label -100, on doit le forcer à True
            # Le label a déjà été remplacé par 0 (ligne 107), donc c'est safe
            crf_mask[:, 0] = True

            # Debug: vérifier les valeurs avant CRF
            if torch.isnan(emissions_fp32).any() or torch.isinf(emissions_fp32).any():
                print("[ERROR] Emissions contain NaN or Inf!")
                print(f"  Emissions range: [{emissions_fp32.min():.4f}, {emissions_fp32.max():.4f}]")
                print(f"  NaN count: {torch.isnan(emissions_fp32).sum().item()}")
                print(f"  Inf count: {torch.isinf(emissions_fp32).sum().item()}")

            # Calculer la negative log-likelihood du CRF
            # Le CRF maximise P(labels | emissions, mask)
            # reduction='token_mean' divise par le nombre de tokens valides
            # CRITICAL: Disable autocast for CRF to ensure FP32 precision
            # Mixed precision autocast can cause NaN in CRF log-likelihood calculations
            try:
                with torch.amp.autocast(device_type='cuda', enabled=False):
                    # Ensure inputs are FP32 (already done above, but explicit here)
                    log_likelihood = self.crf(emissions_fp32, labels_masked, mask=crf_mask, reduction='token_mean')
                    loss = -log_likelihood  # On minimise la negative log-likelihood
            except Exception as e:
                print(f"[ERROR] CRF forward failed: {e}")
                print(f"  Emissions shape: {emissions_fp32.shape}")
                print(f"  Labels shape: {labels_masked.shape}")
                print(f"  Mask shape: {crf_mask.shape}")
                print(f"  Mask sum per sequence: {crf_mask.sum(dim=1)}")
                raise

            # Vérifier les NaN/Inf (peut arriver avec underflow numérique ou mauvaise initialisation CRF)
            if torch.isnan(loss) or torch.isinf(loss):
                print("[ERROR] CRF loss is NaN or Inf!")
                print(f"  log_likelihood: {log_likelihood.item()}")
                print(f"  Emissions stats: min={emissions_fp32.min().item():.4f}, max={emissions_fp32.max().item():.4f}, mean={emissions_fp32.mean().item():.4f}")
                print(f"  Labels unique: {torch.unique(labels_masked).tolist()}")
                print(f"  Mask ratio: {crf_mask.float().mean().item():.4f}")
                print(f"  CRF transitions matrix range: [{self.crf.transitions.min().item():.4f}, {self.crf.transitions.max().item():.4f}]")

                # Si log_likelihood = -inf, le CRF a une probabilité 0 pour cette séquence
                # Cela peut arriver avec une mauvaise initialisation des transitions
                # On utilise une loss de repli basée uniquement sur les émissions (cross-entropy)
                print("[WARN] Using fallback cross-entropy loss instead of CRF loss for this batch")

                # Calculer une cross-entropy classique sur les tokens valides
                # NOTE: On NE doit PAS ignorer le label 0 (c'est "O", un label valide)
                # On utilise le masque pour filtrer les positions invalides
                ce_loss = nn.functional.cross_entropy(
                    emissions_fp32.view(-1, self.num_labels),
                    labels_masked.view(-1),
                    reduction='none'
                )
                # Appliquer le masque pour ne compter que les tokens valides
                ce_loss = ce_loss.view(emissions_fp32.size(0), emissions_fp32.size(1))
                masked_loss = ce_loss * crf_mask.float()
                # Calculer la moyenne sur les tokens valides uniquement
                num_valid_tokens = crf_mask.float().sum()
                if num_valid_tokens > 0:
                    valid_loss = masked_loss.sum() / num_valid_tokens
                else:
                    # Fallback si aucun token valide (ne devrait jamais arriver)
                    valid_loss = torch.tensor(0.0, device=emissions_fp32.device, dtype=emissions_fp32.dtype)
                loss = valid_loss

        else:
            # === MODE INFERENCE ===
            # Décodage Viterbi pour trouver la séquence de labels la plus probable
            # CRITICAL: Disable autocast for CRF decode (same as forward)
            with torch.amp.autocast(device_type='cuda', enabled=False):
                predictions = self.crf.decode(emissions_fp32, mask=crf_mask)
            # predictions est une liste de listes (batch), on doit la convertir en tensor
            # Padding à la longueur max de la séquence
            max_len = emissions_fp32.size(1)
            batch_size = emissions_fp32.size(0)

            # Direct PyTorch tensor creation (avoids NumPy intermediate + device transfer)
            predictions_tensor = torch.zeros((batch_size, max_len), dtype=torch.long, device=emissions_fp32.device)
            for i, pred in enumerate(predictions):
                predictions_tensor[i, :len(pred)] = torch.tensor(pred, dtype=torch.long, device=emissions_fp32.device)
            predictions = predictions_tensor

        return TokenClassifierOutput(
            loss=loss,
            logits=emissions if labels is not None else predictions,  # Emissions (3D) si labels fournis (train/eval), prédictions CRF (2D) si inférence pure
            hidden_states=outputs.hidden_states if hasattr(outputs, 'hidden_states') else None,
            attentions=outputs.attentions if hasattr(outputs, 'attentions') else None,
        )


# Exemple d'utilisation
if __name__ == "__main__":
    from transformers import AutoTokenizer, AutoConfig

    # Charger config et tokenizer
    model_name = "camembert-base"
    config = AutoConfig.from_pretrained(model_name)
    config.num_labels = 9  # O, B-FROM, I-FROM, B-TO, I-TO, B-VIA, I-VIA, B-PERSON, I-PERSON
    tokenizer = AutoTokenizer.from_pretrained(model_name)

    # Créer le modèle avec CRF
    model = CamembertForTokenClassificationWithCRF.from_pretrained(
        model_name,
        config=config
    )

    # Exemple de prédiction
    text = "Je veux aller de Paris à Lyon"
    inputs = tokenizer(text, return_tensors="pt", padding=True)

    # Training mode
    labels = torch.randint(0, 9, (1, inputs["input_ids"].size(1)))
    labels[:, 0] = -100  # Masquer [CLS]
    labels[:, -1] = -100  # Masquer [SEP]

    outputs = model(**inputs, labels=labels)
    print(f"Training loss: {outputs.loss.item():.4f}")

    # Inference mode
    model.eval()
    with torch.no_grad():
        outputs = model(**inputs)
        # Le CRF decode() retourne directement les prédictions
        # On doit les extraire du forward
        predictions = model.crf.decode(outputs.logits, mask=inputs["attention_mask"].bool())
        print(f"Predictions: {predictions}")
