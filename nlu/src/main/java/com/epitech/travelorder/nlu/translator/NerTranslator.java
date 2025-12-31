package com.epitech.travelorder.nlu.translator;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.TranslatorContext;
import com.epitech.travelorder.common.model.BaseTranslator;
import com.epitech.travelorder.nlu.dto.NerEntity;
import com.epitech.travelorder.nlu.dto.NerResult;
import com.epitech.travelorder.nlu.model.NerLabel;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.epitech.travelorder.nlu.util.TokenizerUtils.tokenize;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

public class NerTranslator extends BaseTranslator<String, NerResult> {

    private static final int MAX_LENGTH = 128;
    private static final NerLabel[] NER_LABELS = NerLabel.values();

    private final HuggingFaceTokenizer tokenizer;

    public NerTranslator(Path artifactsPath) {
        try {
            Path tokenizerPath = artifactsPath.resolve("ner_tokenizer");
            this.tokenizer = HuggingFaceTokenizer.newInstance(
                tokenizerPath,
                Map.of("maxLength", String.valueOf(MAX_LENGTH))
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to load tokenizer from: " + artifactsPath, e);
        }
    }

    @Override
    public NDList processInput(TranslatorContext ctx, String input) {
        NDManager manager = ctx.getNDManager();
        ctx.setAttachment("input_text", input);
        return tokenize(input, manager, tokenizer);
    }

    @Override
    public NerResult processOutput(TranslatorContext ctx, NDList list) {
        NDArray output = list.singletonOrThrow();

        if (output.getShape().dimension() == 3) {
            output = output.get(0);
        }

        long[] shape = output.getShape().getShape();
        int seqLength = (int) shape[0];

        Encoding encoding = tokenizer.encode(ctx.getAttachment("input_text").toString());
        String[] tokens = encoding.getTokens();

        List<NerEntity> entities = emptyIfNull(extractEntities(output, tokens, seqLength));

        return new NerResult(entities);
    }

    private List<NerEntity> extractEntities(NDArray output, String[] tokens, int seqLength) {
        List<NerEntity> entities = new ArrayList<>();

        StringBuilder currentEntity = null;
        NerLabel currentLabel = null;
        int entityStart = -1;
        float entityConfidence = 0.0f;

        for (int i = 0; i < seqLength && i < tokens.length; i++) {
            NDArray tokenLogits = output.get(i);
            int predictedLabelIdx = argmax(tokenLogits);

            NDArray probabilities = tokenLogits.softmax(0);
            float confidence = probabilities.getFloat(predictedLabelIdx);

            NerLabel label = predictedLabelIdx < NER_LABELS.length
                ? NER_LABELS[predictedLabelIdx]
                : NerLabel.O;

            if (label.isBegin()) {
                if (currentEntity != null) {
                    entities.add(new NerEntity(
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

            } else if (label.isInside() && currentEntity != null) {
                currentEntity.append(tokens[i].replace("▁", " "));
                entityConfidence = Math.max(entityConfidence, confidence);

            } else if (label.isOutside() || !label.isInside()) {
                if (currentEntity != null) {
                    entities.add(new NerEntity(
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
            entities.add(new NerEntity(
                currentEntity.toString().trim(),
                currentLabel,
                entityConfidence,
                entityStart,
                seqLength - 1
            ));
        }

        return entities;
    }
}

