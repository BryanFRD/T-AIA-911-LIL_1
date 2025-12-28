package com.epitech.travelorder.nlu;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import com.epitech.travelorder.common.ml.BaseTranslator;

/**
 * Translator for Intent Classification model
 * This is a simple example - you'll need to adapt it to your actual model's input/output format
 */
public class IntentTranslator extends BaseTranslator<String, IntentResult> {

    // Intent labels from enum
    private static final IntentLabel[] INTENT_LABELS = IntentLabel.values();

    @Override
    public NDList processInput(ai.djl.translate.TranslatorContext ctx, String input) throws Exception {
        NDManager manager = ctx.getNDManager();

        // TODO: Implement actual tokenization and encoding
        // This is a placeholder - you need to:
        // 1. Tokenize the text
        // 2. Convert tokens to IDs
        // 3. Create attention masks
        // 4. Create NDArray with proper shape

        // For now, create a dummy tensor (you'll replace this with actual preprocessing)
        long[] inputIds = new long[]{101, 2023, 2003, 1037, 6876, 102}; // Example token IDs
        NDArray inputTensor = manager.create(inputIds);

        return new NDList(inputTensor);
    }

    @Override
    public IntentResult processOutput(ai.djl.translate.TranslatorContext ctx, NDList list) throws Exception {
        // Get the output tensor (logits)
        NDArray output = list.singletonOrThrow();

        // Get predicted class (argmax)
        int predictedClass = argmax(output);

        // Get confidence (softmax probability of predicted class)
        NDArray probabilities = output.softmax(0);
        float confidence = probabilities.getFloat(predictedClass);

        // Map to intent label
        IntentLabel intent = predictedClass < INTENT_LABELS.length
            ? INTENT_LABELS[predictedClass]
            : IntentLabel.OTHER;

        return new IntentResult(intent, confidence);
    }
}
