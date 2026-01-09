package com.epitech.travelorder.common.domain.nlu;

import com.epitech.travelorder.common.domain.nlu.intent.Intent;
import com.epitech.travelorder.common.domain.nlu.ner.Ner;

public record Nlu(
    Intent intent,
    Ner ner
) {

    @Override
    public String toString() {
        return String.format("NluResult{intentLabel=%s, entities=%s}",
            intent,
            ner
        );
    }
}

