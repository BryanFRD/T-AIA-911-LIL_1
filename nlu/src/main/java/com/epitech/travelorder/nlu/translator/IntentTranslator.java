package com.epitech.travelorder.nlu.translator;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.TranslatorContext;
import com.epitech.travelorder.common.domain.enumeration.IntentLabel;
import com.epitech.travelorder.common.domain.mlflow.BaseTranslator;
import com.epitech.travelorder.nlu.util.TokenizerUtils;
import com.epitech.travelorder.common.domain.nlu.intent.Intent;

import java.nio.file.Path;
import java.util.Map;

public class IntentTranslator extends BaseTranslator<String, Intent> {

    private static final int MAX_LENGTH = 128;
    private static final IntentLabel[] INTENT_LABELS = IntentLabel.values();

    private final HuggingFaceTokenizer tokenizer;

    public IntentTranslator(Path artifactsPath) {
        try {
            Path tokenizerPath = artifactsPath.resolve("intent_tokenizer");
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
        return TokenizerUtils.tokenize(input, manager, tokenizer);
    }

    @Override
    public Intent processOutput(TranslatorContext ctx, NDList list) {
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

        return new Intent(intent, confidence);
    }
}

