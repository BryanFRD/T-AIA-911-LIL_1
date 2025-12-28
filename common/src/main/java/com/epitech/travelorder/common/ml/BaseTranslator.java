package com.epitech.travelorder.common.ml;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;

/**
 * Abstract base translator providing common functionality for model input/output processing
 *
 * @param <I> Input type
 * @param <O> Output type
 */
public abstract class BaseTranslator<I, O> implements Translator<I, O> {

    @Override
    public Batchifier getBatchifier() {
        return Batchifier.STACK;
    }

    /**
     * Convert input to NDList (tensor) for model inference
     */
    @Override
    public abstract NDList processInput(TranslatorContext ctx, I input) throws Exception;

    /**
     * Convert model output NDList to result object
     */
    @Override
    public abstract O processOutput(TranslatorContext ctx, NDList list) throws Exception;

    /**
     * Helper method to create a tensor from a float array
     */
    protected NDArray createTensor(NDManager manager, float[] data, long... shape) {
        return manager.create(data, shape);
    }

    /**
     * Helper method to create a tensor from a long array
     */
    protected NDArray createTensor(NDManager manager, long[] data, long... shape) {
        return manager.create(data, shape);
    }

    /**
     * Helper method to get the argmax (index of maximum value) from a tensor
     */
    protected int argmax(NDArray array) {
        return Math.toIntExact(array.argMax().getLong());
    }

    /**
     * Helper method to convert NDArray to float array
     */
    protected float[] toFloatArray(NDArray array) {
        return array.toFloatArray();
    }

    /**
     * Helper method to convert NDArray to long array
     */
    protected long[] toLongArray(NDArray array) {
        return array.toLongArray();
    }
}
