package com.craftbook.calculator.application;

import com.craftbook.calculator.domain.CalculationResult.Stacks;

final class StackConverter {
    private StackConverter() { }

    static Stacks toStacks(long quantity, int stackSize) {
        return new Stacks(quantity / stackSize, (int) (quantity % stackSize));
    }
}
