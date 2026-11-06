package com.craftbook.calculator.domain;

import java.util.List;
import java.util.Set;

/** Receita que produz {@code outputQuantity} unidades de um material por operação. */
public record Recipe(
        String id,
        String outputMaterialId,
        int outputQuantity,
        List<Ingredient> ingredients,
        Set<Edition> editions,
        GameVersion minVersion,
        GameVersion maxVersion) {

    public record Ingredient(String materialId, int quantity) {
        public Ingredient {
            if (materialId == null || materialId.isBlank() || quantity < 1) {
                throw new IllegalArgumentException("Ingrediente inválido");
            }
        }
    }

    public Recipe {
        if (id == null || id.isBlank() || outputMaterialId == null || outputMaterialId.isBlank()) {
            throw new IllegalArgumentException("Receita incompleta");
        }
        if (outputQuantity < 1) {
            throw new IllegalArgumentException("Receita deve produzir ao menos 1 unidade");
        }
        if (ingredients == null || ingredients.isEmpty() || editions == null || editions.isEmpty()) {
            throw new IllegalArgumentException("Receita sem ingredientes ou sem edições");
        }
        if (minVersion != null && maxVersion != null && minVersion.compareTo(maxVersion) > 0) {
            throw new IllegalArgumentException("minVersion maior que maxVersion");
        }
        ingredients = List.copyOf(ingredients);
        editions = Set.copyOf(editions);
    }

    public boolean supports(Edition edition, GameVersion version) {
        return editions.contains(edition)
                && (minVersion == null || version.compareTo(minVersion) >= 0)
                && (maxVersion == null || version.compareTo(maxVersion) <= 0);
    }
}
