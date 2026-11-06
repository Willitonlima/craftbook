package com.craftbook.calculator.application;

import com.craftbook.calculator.domain.RecipeCycleException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Ordenação topológica (algoritmo de Kahn) do grafo produto → ingredientes.
 * A ordem devolvida garante que todo consumidor de um material vem antes dele.
 */
final class CycleDetector {

    private CycleDetector() { }

    /** @param edges todo nó deve ser chave do mapa (com conjunto vazio se não tiver ingredientes). */
    static List<String> topologicalOrder(Map<String, Set<String>> edges) {
        Map<String, Integer> inDegree = new HashMap<>();
        edges.keySet().forEach(n -> inDegree.put(n, 0));
        edges.values().forEach(targets -> targets.forEach(t -> inDegree.merge(t, 1, Integer::sum)));

        TreeSet<String> ready = new TreeSet<>(); // TreeSet => ordem determinística
        inDegree.forEach((n, d) -> { if (d == 0) ready.add(n); });

        List<String> order = new ArrayList<>();
        while (!ready.isEmpty()) {
            String node = ready.pollFirst();
            order.add(node);
            for (String target : edges.getOrDefault(node, Set.of())) {
                if (inDegree.merge(target, -1, Integer::sum) == 0) {
                    ready.add(target);
                }
            }
        }
        if (order.size() < inDegree.size()) {
            List<String> stuck = new ArrayList<>(inDegree.keySet());
            stuck.removeAll(order);
            stuck.sort(String::compareTo);
            throw new RecipeCycleException(stuck);
        }
        return order;
    }
}
