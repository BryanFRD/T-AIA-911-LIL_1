package com.epitech.travelorder.nlu.infrastructure.util;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public final class TokenizerUtils {

    public static NDList tokenize(String input, NDManager manager, HuggingFaceTokenizer tokenizer) {
        Encoding encoding = tokenizer.encode(input);
        long[] inputIds = encoding.getIds();
        long[] attentionMask = encoding.getAttentionMask();

        NDArray inputIdsTensor = manager.create(inputIds);
        NDArray attentionMaskTensor = manager.create(attentionMask);

        return new NDList(inputIdsTensor, attentionMaskTensor);
    }
}

