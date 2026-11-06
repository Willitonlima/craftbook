package com.craftbook.calculator.domain;

import java.util.List;

/**
 * Resultado do cálculo.
 *
 * @param exact false quando há {@link Issue}s: o resultado é uma estimativa parcial, não um valor exato.
 */
public record CalculationResult(List<MaterialLine> lines, List<Issue> issues, boolean exact) {

    public record Stacks(long fullStacks, int remainder) { }

    /**
     * @param required       demanda total (construção + ingredientes de receitas expandidas)
     * @param fromInventory  quanto da demanda é coberto pelo inventário
     * @param missing        required - fromInventory
     * @param toGather       o que ainda precisa ser obtido (0 quando o faltante será fabricado)
     * @param craftOperations operações de receita necessárias para cobrir o faltante
     * @param craftedUnits   unidades produzidas pelas operações
     * @param surplus        craftedUnits - missing
     */
    public record MaterialLine(
            String materialId, String name, String category, int stackSize,
            long required, long fromInventory, long missing, long toGather,
            long craftOperations, long craftedUnits, long surplus,
            Stacks requiredStacks, Stacks missingStacks) { }

    public enum IssueType { RECIPE_NOT_REGISTERED, RECIPE_INCOMPATIBLE }

    public record Issue(IssueType type, String materialId, String message) { }
}
