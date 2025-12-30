package com.epitech.travelorder.nlu;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import com.epitech.travelorder.common.model.BaseTranslator;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Translator for Named Entity Recognition model using CamemBERT tokenizer
 */
public class NERTranslator extends BaseTranslator<String, NERResult> {

    private static final int MAX_LENGTH = 128;
    private static final NERLabel[] NER_LABELS = NERLabel.values();

    private final HuggingFaceTokenizer tokenizer;

    public NERTranslator(Path artifactsPath) {
        try {
            Path tokenizerPath = artifactsPath.resolve("ner_tokenizer");
            this.tokenizer = HuggingFaceTokenizer.newInstance(tokenizerPath, Map.of("maxLength", String.valueOf(MAX_LENGTH)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load tokenizer from artifacts: " + artifactsPath, e);
        }
    }

    @Override
    public NDList processInput(ai.djl.translate.TranslatorContext ctx, String input) {
        NDManager manager = ctx.getNDManager();
        ctx.setAttachment("input_text", input);

        return getNdArrays(input, manager, tokenizer);
    }

    static NDList getNdArrays(final String input, final NDManager manager, final HuggingFaceTokenizer tokenizer) {
        Encoding encoding = tokenizer.encode(input);
        long[] inputIds = encoding.getIds();
        long[] attentionMask = encoding.getAttentionMask();

        NDArray inputIdsTensor = manager.create(inputIds).reshape(1, inputIds.length);
        NDArray attentionMaskTensor = manager.create(attentionMask).reshape(1, attentionMask.length);

        return new NDList(inputIdsTensor, attentionMaskTensor);
    }

    @Override
    public NERResult processOutput(ai.djl.translate.TranslatorContext ctx, NDList list) {
        NDArray output = list.singletonOrThrow();

        if (output.getShape().dimension() == 3) {
            output = output.get(0);
        }

        long[] shape = output.getShape().getShape();
        int seqLength = (int) shape[0];

        List<NERResult.Entity> entities = new ArrayList<>();

        Encoding encoding = tokenizer.encode(ctx.getAttachment("input_text").toString());
        String[] tokens = encoding.getTokens();

        StringBuilder currentEntity = null;
        NERLabel currentLabel = null;
        int entityStart = -1;
        float entityConfidence = 0.0f;

        for (int i = 0; i < seqLength && i < tokens.length; i++) {
            NDArray tokenLogits = output.get(i);
            int predictedLabelIdx = argmax(tokenLogits);

            NDArray probabilities = tokenLogits.softmax(0);
            float confidence = probabilities.getFloat(predictedLabelIdx);

            NERLabel label = predictedLabelIdx < NER_LABELS.length
                ? NER_LABELS[predictedLabelIdx]
                : NERLabel.O;

            if (label.getLabel().startsWith("B-")) {
                if (currentEntity != null) {
                    entities.add(new NERResult.Entity(
                        currentEntity.toString().trim(),
                        currentLabel,
                        entityConfidence,
                        entityStart,
                        i - 1
                    ));
                }

                currentEntity = new StringBuilder(tokens[i].replace("▁", " "));
                currentLabel = label;
                entityStart = i;
                entityConfidence = confidence;

            } else if (label.getLabel().startsWith("I-") && currentEntity != null) {
                currentEntity.append(tokens[i].replace("▁", " "));
                entityConfidence = Math.max(entityConfidence, confidence);

            } else if (label == NERLabel.O || !label.getLabel().startsWith("I-")) {
                if (currentEntity != null) {
                    entities.add(new NERResult.Entity(
                        currentEntity.toString().trim(),
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

        if (currentEntity != null) {
            entities.add(new NERResult.Entity(
                currentEntity.toString().trim(),
                currentLabel,
                entityConfidence,
                entityStart,
                seqLength - 1
            ));
        }

        return new NERResult(entities);
    }
}
