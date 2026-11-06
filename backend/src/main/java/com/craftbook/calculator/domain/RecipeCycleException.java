package com.craftbook.calculator.domain;

import java.util.List;

@SuppressWarnings("serial")
public class RecipeCycleException extends RuntimeException {
    private final List<String> materialIds;

    public RecipeCycleException(List<String> materialIds) {
        super("Dependência cíclica entre receitas envolvendo: " + String.join(", ", materialIds));
        this.materialIds = List.copyOf(materialIds);
    }

    public List<String> materialIds() { return materialIds; }
}
