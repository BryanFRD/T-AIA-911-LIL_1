package com.epitech.travelorder.nlu;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import com.epitech.travelorder.common.ml.BaseTranslator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Translator for Named Entity Recognition model using CamemBERT tokenizer
 */
public class NERTranslator extends BaseTranslator<String, NERResult> {

    private static final int MAX_LENGTH = 128;

    // NER labels from enum (BIO format)
    private static final NERLabel[] NER_LABELS = NERLabel.values();

    private HuggingFaceTokenizer tokenizer;

    public NERTranslator(Path artifactsPath) {
        try {
            // Load tokenizer from MLflow artifacts (ner_tokenizer directory)
            Path tokenizerPath = artifactsPath.resolve("ner_tokenizer");
            this.tokenizer = HuggingFaceTokenizer.newInstance(tokenizerPath, Map.of("maxLength", String.valueOf(MAX_LENGTH)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load tokenizer from artifacts: " + artifactsPath, e);
        }
    }

    @Override
    public NDList processInput(ai.djl.translate.TranslatorContext ctx, String input) throws Exception {
        NDManager manager = ctx.getNDManager();

        // Store input text for later use in processOutput
        ctx.setAttachment("input_text", input);

        // Tokenize the input text
        Encoding encoding = tokenizer.encode(input);

        // Get input IDs and attention mask
        long[] inputIds = encoding.getIds();
        long[] attentionMask = encoding.getAttentionMask();

        // Create NDArrays with shape [seq_len] and add batch dimension to get [1, seq_len]
        NDArray inputIdsTensor = manager.create(inputIds).reshape(1, inputIds.length);
        NDArray attentionMaskTensor = manager.create(attentionMask).reshape(1, attentionMask.length);

        // Return both input_ids and attention_mask as required by the ONNX model
        return new NDList(inputIdsTensor, attentionMaskTensor);
    }

    @Override
    public NERResult processOutput(ai.djl.translate.TranslatorContext ctx, NDList list) throws Exception {
        // Get the output tensor (logits for each token)
        // Output shape: [batch_size, sequence_length, num_labels]
        NDArray output = list.singletonOrThrow();

        // Remove batch dimension: [sequence_length, num_labels]
        if (output.getShape().dimension() == 3) {
            output = output.get(0);
        }

        long[] shape = output.getShape().getShape();
        int seqLength = (int) shape[0];

        List<NERResult.Entity> entities = new ArrayList<>();

        // Get the original tokens for entity text extraction
        Encoding encoding = tokenizer.encode(ctx.getAttachment("input_text").toString());
        String[] tokens = encoding.getTokens();

        // Decode BIO tags and extract entities
        String currentEntity = null;
        NERLabel currentLabel = null;
        int entityStart = -1;
        float entityConfidence = 0.0f;

        for (int i = 0; i < seqLength && i < tokens.length; i++) {
            // Get predictions for this token
            NDArray tokenLogits = output.get(i);
            int predictedLabelIdx = argmax(tokenLogits);

            // Get confidence (softmax probability)
            NDArray probabilities = tokenLogits.softmax(0);
            float confidence = probabilities.getFloat(predictedLabelIdx);

            // Map to NER label
            NERLabel label = predictedLabelIdx < NER_LABELS.length
                ? NER_LABELS[predictedLabelIdx]
                : NERLabel.O;

            // BIO tag decoding
            if (label.getLabel().startsWith("B-")) {
                // Save previous entity if exists
                if (currentEntity != null) {
                    entities.add(new NERResult.Entity(
                        currentEntity.trim(),
                        currentLabel,
                        entityConfidence,
                        entityStart,
                        i - 1
                    ));
                }

                // Start new entity
                currentEntity = tokens[i].replace("▁", " ");
                currentLabel = label;
                entityStart = i;
                entityConfidence = confidence;

            } else if (label.getLabel().startsWith("I-") && currentEntity != null) {
                // Continue current entity
                currentEntity += tokens[i].replace("▁", " ");
                entityConfidence = Math.max(entityConfidence, confidence);

            } else if (label == NERLabel.O || !label.getLabel().startsWith("I-")) {
                // End current entity
                if (currentEntity != null) {
                    entities.add(new NERResult.Entity(
                        currentEntity.trim(),
                        currentLabel,
                        entityConfidence,
                        entityStart,
                        i - 1
                    ));
                    currentEntity = null;
                    currentLabel = null;
                }
            }
        }

        // Add last entity if exists
        if (currentEntity != null) {
            entities.add(new NERResult.Entity(
                currentEntity.trim(),
                currentLabel,
                entityConfidence,
                entityStart,
                seqLength - 1
            ));
        }

        return new NERResult(entities);
    }
}
