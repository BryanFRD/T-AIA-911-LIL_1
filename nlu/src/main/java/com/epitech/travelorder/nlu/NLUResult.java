package com.epitech.travelorder.nlu;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Combined NLU result containing both intent and entities
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class NLUResult {
    private IntentResult intent;
    private NERResult entities;
}
