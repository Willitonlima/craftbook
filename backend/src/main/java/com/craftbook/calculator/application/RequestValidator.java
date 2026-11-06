package com.craftbook.calculator.application;

import com.craftbook.calculator.domain.CalculationRequest;
import com.craftbook.calculator.domain.InvalidCalculationException;
import com.craftbook.calculator.domain.Material;
import com.craftbook.calculator.domain.MaterialRequirement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Valida limites e referências da requisição, reportando todas as violações juntas. */
final class RequestValidator {

    static final int MAX_BUILD_QUANTITY = 1_000;
    static final int MAX_ITEM_QUANTITY = 10_000_000;
    static final int MAX_REQUIREMENTS = 500;

    private final Map<String, Material> materials;

    RequestValidator(Map<String, Material> materials) {
        this.materials = materials;
    }

    void validate(CalculationRequest request) {
        List<String> violations = new ArrayList<>();

        if (request.edition() == null) violations.add("edition é obrigatória");
        if (request.version() == null) violations.add("version é obrigatória");
        if (request.buildQuantity() < 1 || request.buildQuantity() > MAX_BUILD_QUANTITY) {
            violations.add("buildQuantity deve estar entre 1 e " + MAX_BUILD_QUANTITY);
        }
        if (request.requirements().isEmpty()) {
            violations.add("requirements não pode ser vazio");
        }
        if (request.requirements().size() > MAX_REQUIREMENTS) {
            violations.add("requirements excede o máximo de " + MAX_REQUIREMENTS);
        }
        for (MaterialRequirement r : request.requirements()) {
            if (r == null || !materials.containsKey(r.materialId())) {
                violations.add("material desconhecido em requirements: " + (r == null ? null : r.materialId()));
            } else if (r.quantity() < 1 || r.quantity() > MAX_ITEM_QUANTITY) {
                violations.add("quantidade de " + r.materialId() + " deve estar entre 1 e " + MAX_ITEM_QUANTITY);
            }
        }
        request.inventory().forEach((id, qty) -> {
            if (!materials.containsKey(id)) {
                violations.add("material desconhecido no inventário: " + id);
            } else if (qty < 0 || qty > MAX_ITEM_QUANTITY) {
                violations.add("inventário de " + id + " deve estar entre 0 e " + MAX_ITEM_QUANTITY);
            }
        });

        if (!violations.isEmpty()) {
            throw new InvalidCalculationException(violations);
        }
    }
}
