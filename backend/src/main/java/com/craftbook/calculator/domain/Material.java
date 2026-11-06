package com.craftbook.calculator.domain;

/**
 * Material do catálogo.
 *
 * @param craftable true quando o material pode ser fabricado; apenas esses materiais são expandidos em receitas
 *                  e geram aviso se não houver receita compatível cadastrada.
 */
public record Material(String id, String name, String category, int stackSize, boolean craftable) {

    public Material {
        if (id == null || id.isBlank() || name == null || name.isBlank() || category == null) {
            throw new IllegalArgumentException("Material incompleto");
        }
        if (stackSize < 1 || stackSize > 64) {
            throw new IllegalArgumentException("Tamanho de stack fora de 1..64: " + stackSize);
        }
    }
}
