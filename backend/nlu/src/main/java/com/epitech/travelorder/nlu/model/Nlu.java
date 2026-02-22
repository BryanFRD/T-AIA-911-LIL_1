package com.epitech.travelorder.nlu.model;

import com.epitech.travelorder.nlu.model.intent.Intent;
import com.epitech.travelorder.nlu.model.ner.Ner;

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

