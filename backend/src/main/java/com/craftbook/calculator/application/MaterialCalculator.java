package com.craftbook.calculator.application;

import com.craftbook.calculator.domain.CalculationRequest;
import com.craftbook.calculator.domain.CalculationResult;
import com.craftbook.calculator.domain.CalculationResult.Issue;
import com.craftbook.calculator.domain.CalculationResult.IssueType;
import com.craftbook.calculator.domain.CalculationResult.MaterialLine;
import com.craftbook.calculator.domain.InvalidCalculationException;
import com.craftbook.calculator.domain.Material;
import com.craftbook.calculator.domain.MaterialRequirement;
import com.craftbook.calculator.domain.Recipe;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Calculadora de materiais. Lógica pura de domínio: sem Spring, JPA ou HTTP.
 *
 * <p>Estratégia (ver docs/calculadora.md):
 * <ol>
 *   <li>consolida as exigências repetidas e multiplica por {@code buildQuantity};</li>
 *   <li>se {@code expandRecipes}, monta o grafo produto→ingredientes dos materiais fabricáveis e o ordena
 *       topologicamente (falha com {@code RecipeCycleException} se houver ciclo);</li>
 *   <li>percorre nessa ordem: cada material é processado UMA vez, já com a demanda total de todos os seus
 *       consumidores; o inventário é descontado uma única vez; o faltante de um material fabricável vira
 *       {@code ceil(faltante / saída)} operações cujos ingredientes entram na demanda dos materiais seguintes.</li>
 * </ol>
 * Excedentes de uma receita não são reaproveitados em outras demandas (isso exigiria otimização global);
 * eles são apenas informados.
 */
public final class MaterialCalculator {

    private final Map<String, Material> materials = new HashMap<>();
    private final RecipeCatalog recipes;
    private final RequestValidator validator;

    public MaterialCalculator(Collection<Material> materials, Collection<Recipe> recipes) {
        materials.forEach(m -> this.materials.put(m.id(), m));
        for (Recipe r : recipes) {
            if (!this.materials.containsKey(r.outputMaterialId())) {
                throw new IllegalArgumentException("Receita " + r.id() + " produz material desconhecido");
            }
            for (Recipe.Ingredient i : r.ingredients()) {
                if (!this.materials.containsKey(i.materialId())) {
                    throw new IllegalArgumentException("Receita " + r.id() + " usa material desconhecido: " + i.materialId());
                }
            }
        }
        this.recipes = new RecipeCatalog(recipes);
        this.validator = new RequestValidator(this.materials);
    }

    public CalculationResult calculate(CalculationRequest request) {
        validator.validate(request);
        try {
            return doCalculate(request);
        } catch (ArithmeticException overflow) {
            throw new InvalidCalculationException(List.of("quantidades fora dos limites permitidos (overflow)"));
        }
    }

    private CalculationResult doCalculate(CalculationRequest request) {
        Map<String, Long> demand = consolidate(request);
        List<Issue> issues = new ArrayList<>();
        Map<String, Recipe> chosen = new HashMap<>();
        List<String> order;

        if (request.expandRecipes()) {
            order = CycleDetector.topologicalOrder(buildGraph(request, demand.keySet(), chosen, issues));
        } else {
            order = new ArrayList<>(new TreeSet<>(demand.keySet()));
        }

        List<MaterialLine> lines = new ArrayList<>();
        for (String id : order) {
            long required = demand.getOrDefault(id, 0L);
            if (required == 0) continue;

            Material material = materials.get(id);
            long inventory = request.inventory().getOrDefault(id, 0);
            long fromInventory = Math.min(required, inventory);
            long missing = required - fromInventory;

            Recipe recipe = chosen.get(id);
            long toGather = missing;
            long operations = 0, crafted = 0, surplus = 0;
            if (recipe != null) {
                toGather = 0;
                if (missing > 0) {
                    operations = Math.ceilDiv(missing, (long) recipe.outputQuantity());
                    crafted = Math.multiplyExact(operations, (long) recipe.outputQuantity());
                    surplus = crafted - missing;
                    for (Recipe.Ingredient ing : recipe.ingredients()) {
                        long needed = Math.multiplyExact(operations, (long) ing.quantity());
                        demand.merge(ing.materialId(), needed, Math::addExact);
                    }
                }
            }
            lines.add(new MaterialLine(
                    id, material.name(), material.category(), material.stackSize(),
                    required, fromInventory, missing, toGather, operations, crafted, surplus,
                    StackConverter.toStacks(required, material.stackSize()),
                    StackConverter.toStacks(missing, material.stackSize())));
        }

        lines.sort(Comparator.comparing(MaterialLine::category).thenComparing(MaterialLine::name));
        return new CalculationResult(List.copyOf(lines), List.copyOf(issues), issues.isEmpty());
    }

    /** Soma exigências repetidas e multiplica pela quantidade de construções. */
    private Map<String, Long> consolidate(CalculationRequest request) {
        Map<String, Long> demand = new TreeMap<>();
        for (MaterialRequirement r : request.requirements()) {
            long total = Math.multiplyExact((long) r.quantity(), (long) request.buildQuantity());
            demand.merge(r.materialId(), total, Math::addExact);
        }
        return demand;
    }

    /** Percorre os materiais alcançáveis, escolhendo receitas compatíveis e registrando limitações. */
    private Map<String, Set<String>> buildGraph(
            CalculationRequest request, Set<String> roots, Map<String, Recipe> chosen, List<Issue> issues) {
        Map<String, Set<String>> edges = new TreeMap<>();
        Deque<String> pending = new ArrayDeque<>(new TreeSet<>(roots));
        Set<String> visited = new HashSet<>();

        while (!pending.isEmpty()) {
            String id = pending.pop();
            if (!visited.add(id)) continue;
            edges.putIfAbsent(id, new TreeSet<>());

            Material material = materials.get(id);
            if (!material.craftable()) continue;

            var recipe = recipes.selectCompatible(id, request.edition(), request.version());
            if (recipe.isPresent()) {
                chosen.put(id, recipe.get());
                for (Recipe.Ingredient ing : recipe.get().ingredients()) {
                    edges.get(id).add(ing.materialId());
                    pending.push(ing.materialId());
                }
            } else if (recipes.hasAnyRecipe(id)) {
                issues.add(new Issue(IssueType.RECIPE_INCOMPATIBLE, id,
                        "Existem receitas para " + material.name() + ", mas nenhuma é compatível com "
                                + request.edition() + " " + request.version()));
            } else {
                issues.add(new Issue(IssueType.RECIPE_NOT_REGISTERED, id,
                        "Receita de " + material.name() + " não cadastrada; o faltante não foi expandido"));
            }
        }
        return edges;
    }
}
