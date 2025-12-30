package com.epitech.travelorder.nlu;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import com.epitech.travelorder.common.model.BaseTranslator;

import java.nio.file.Path;
import java.util.Map;

import static com.epitech.travelorder.nlu.NERTranslator.getNdArrays;

public class IntentTranslator extends BaseTranslator<String, IntentResult> {

    private static final int MAX_LENGTH = 128;
    private static final IntentLabel[] INTENT_LABELS = IntentLabel.values();

    private final HuggingFaceTokenizer tokenizer;

    public IntentTranslator(Path artifactsPath) {
        try {
            Path tokenizerPath = artifactsPath.resolve("intent_tokenizer");
            this.tokenizer = HuggingFaceTokenizer.newInstance(tokenizerPath, Map.of("maxLength", String.valueOf(MAX_LENGTH)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load tokenizer from artifacts: " + artifactsPath, e);
        }
    }

    @Override
    public NDList processInput(ai.djl.translate.TranslatorContext ctx, String input) {
        NDManager manager = ctx.getNDManager();
        return getNdArrays(input, manager, tokenizer);
    }

    @Override
    public IntentResult processOutput(ai.djl.translate.TranslatorContext ctx, NDList list) {
        NDArray output = list.singletonOrThrow();

        if (output.getShape().dimension() > 1) {
            output = output.get(0);
        }

        int predictedClass = argmax(output);
        NDArray probabilities = output.softmax(0);
        float confidence = probabilities.getFloat(predictedClass);

        IntentLabel intent = predictedClass < INTENT_LABELS.length
            ? INTENT_LABELS[predictedClass]
            : IntentLabel.OTHER;

        return new IntentResult(intent, confidence);
    }
}
