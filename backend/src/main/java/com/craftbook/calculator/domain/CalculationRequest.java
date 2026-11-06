package com.craftbook.calculator.domain;

import java.util.List;
import java.util.Map;

/**
 * Entrada do cálculo. A validação de limites é feita por {@code RequestValidator}, não aqui,
 * para que todas as violações sejam reportadas de uma vez.
 */
public record CalculationRequest(
        Edition edition,
        GameVersion version,
        int buildQuantity,
        List<MaterialRequirement> requirements,
        Map<String, Integer> inventory,
        boolean expandRecipes) {

    public CalculationRequest {
        requirements = requirements == null ? List.of() : List.copyOf(requirements);
        inventory = inventory == null ? Map.of() : Map.copyOf(inventory);
    }
}
