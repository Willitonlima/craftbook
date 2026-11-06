package com.craftbook.calculator.domain;

/** Quantidade de um material exigida por UMA construção (soma das etapas). */
public record MaterialRequirement(String materialId, int quantity) { }
