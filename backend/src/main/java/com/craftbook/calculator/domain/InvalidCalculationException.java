package com.craftbook.calculator.domain;

import java.util.List;

@SuppressWarnings("serial")
public class InvalidCalculationException extends RuntimeException {
    private final List<String> violations;

    public InvalidCalculationException(List<String> violations) {
        super("Requisição de cálculo inválida: " + String.join("; ", violations));
        this.violations = List.copyOf(violations);
    }

    public List<String> violations() { return violations; }
}
