package com.epitech.travelorder.nlu;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Combined NLU result containing both intent and entities
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NLUResult {
    private IntentResult intent;
    private NERResult entities;

    @Override
    public String toString() {
        return String.format("NLU Result: %s | %s", intent, entities);
    }
}
