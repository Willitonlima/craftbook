package com.craftbook.calculator.domain;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/** Versão numérica do jogo (ex.: 1.21.4), comparável segmento a segmento. */
public record GameVersion(List<Integer> parts) implements Comparable<GameVersion> {

    public GameVersion {
        if (parts == null || parts.isEmpty()) {
            throw new IllegalArgumentException("Versão sem segmentos");
        }
        parts = List.copyOf(parts);
    }

    public static GameVersion parse(String text) {
        if (text == null || !text.matches("\\d{1,4}(\\.\\d{1,4}){1,3}")) {
            throw new IllegalArgumentException("Versão inválida: " + text);
        }
        return new GameVersion(Arrays.stream(text.split("\\.")).map(Integer::parseInt).toList());
    }

    @Override
    public int compareTo(GameVersion other) {
        int size = Math.max(parts.size(), other.parts.size());
        for (int i = 0; i < size; i++) {
            int a = i < parts.size() ? parts.get(i) : 0;
            int b = i < other.parts.size() ? other.parts.get(i) : 0;
            if (a != b) {
                return Integer.compare(a, b);
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        return parts.stream().map(String::valueOf).collect(Collectors.joining("."));
    }
}
