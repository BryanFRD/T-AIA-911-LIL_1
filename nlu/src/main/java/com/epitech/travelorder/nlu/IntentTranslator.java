package com.epitech.travelorder.nlu;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import com.epitech.travelorder.common.ml.BaseTranslator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * Translator for Intent Classification model using CamemBERT tokenizer
 */
public class IntentTranslator extends BaseTranslator<String, IntentResult> {

    private static final int MAX_LENGTH = 128;

    // Intent labels from enum
    private static final IntentLabel[] INTENT_LABELS = IntentLabel.values();

    private HuggingFaceTokenizer tokenizer;

    public IntentTranslator(Path artifactsPath) {
        try {
            // Load tokenizer from MLflow artifacts (intent_tokenizer directory)
            Path tokenizerPath = artifactsPath.resolve("intent_tokenizer");
            this.tokenizer = HuggingFaceTokenizer.newInstance(tokenizerPath, Map.of("maxLength", String.valueOf(MAX_LENGTH)));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load tokenizer from artifacts: " + artifactsPath, e);
        }
    }

    @Override
    public NDList processInput(ai.djl.translate.TranslatorContext ctx, String input) throws Exception {
        NDManager manager = ctx.getNDManager();

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
    public IntentResult processOutput(ai.djl.translate.TranslatorContext ctx, NDList list) throws Exception {
        // Get the output tensor (logits) - shape: [batch_size, num_classes]
        NDArray output = list.singletonOrThrow();

        // Remove batch dimension if present
        if (output.getShape().dimension() > 1) {
            output = output.get(0);
        }

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
