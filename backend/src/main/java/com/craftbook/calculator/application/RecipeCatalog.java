package com.craftbook.calculator.application;

import com.craftbook.calculator.domain.Edition;
import com.craftbook.calculator.domain.GameVersion;
import com.craftbook.calculator.domain.Recipe;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Índice de receitas por material produzido, com seleção por compatibilidade de edição/versão. */
final class RecipeCatalog {

    private final Map<String, List<Recipe>> byOutput = new HashMap<>();

    RecipeCatalog(Collection<Recipe> recipes) {
        recipes.stream()
                .sorted(Comparator.comparing(Recipe::id)) // ordem determinística
                .forEach(r -> byOutput.computeIfAbsent(r.outputMaterialId(), k -> new java.util.ArrayList<>()).add(r));
    }

    boolean hasAnyRecipe(String materialId) {
        return byOutput.containsKey(materialId);
    }

    /** Primeira receita (por id) compatível com a edição e a versão; vazio se não houver. */
    Optional<Recipe> selectCompatible(String materialId, Edition edition, GameVersion version) {
        return byOutput.getOrDefault(materialId, List.of()).stream()
                .filter(r -> r.supports(edition, version))
                .findFirst();
    }
}
