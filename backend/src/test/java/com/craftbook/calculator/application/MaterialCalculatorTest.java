package com.craftbook.calculator.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.craftbook.calculator.domain.CalculationRequest;
import com.craftbook.calculator.domain.CalculationResult;
import com.craftbook.calculator.domain.CalculationResult.IssueType;
import com.craftbook.calculator.domain.CalculationResult.MaterialLine;
import com.craftbook.calculator.domain.Edition;
import com.craftbook.calculator.domain.GameVersion;
import com.craftbook.calculator.domain.InvalidCalculationException;
import com.craftbook.calculator.domain.Material;
import com.craftbook.calculator.domain.MaterialRequirement;
import com.craftbook.calculator.domain.Recipe;
import com.craftbook.calculator.domain.RecipeCycleException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Dados de teste são fictícios e servem apenas para validar a lógica; não representam receitas verificadas. */
class MaterialCalculatorTest {

    private static final GameVersion V121 = GameVersion.parse("1.21.4");

    private static final List<Material> MATERIALS = List.of(
            new Material("cobblestone", "Cobblestone", "Blocos", 64, false),
            new Material("log", "Log", "Madeira", 64, false),
            new Material("planks", "Planks", "Madeira", 64, true),
            new Material("stick", "Stick", "Madeira", 64, true),
            new Material("chest", "Chest", "Armazenamento", 64, true),
            new Material("lantern", "Lantern", "Iluminação", 64, true),
            new Material("a", "A", "Teste", 64, true),
            new Material("b", "B", "Teste", 64, true));

    private static Recipe recipe(String id, String out, int qty, Set<Edition> eds, GameVersion min, Recipe.Ingredient... ings) {
        return new Recipe(id, out, qty, List.of(ings), eds, min, null);
    }

    private static final Set<Edition> BOTH = Set.of(Edition.JAVA, Edition.BEDROCK);

    private static final List<Recipe> RECIPES = List.of(
            recipe("r-planks", "planks", 4, BOTH, null, new Recipe.Ingredient("log", 1)),
            recipe("r-stick", "stick", 4, BOTH, null, new Recipe.Ingredient("planks", 2)),
            recipe("r-chest", "chest", 1, BOTH, null, new Recipe.Ingredient("planks", 8)));

    private static MaterialCalculator calculator() { return new MaterialCalculator(MATERIALS, RECIPES); }

    private static CalculationRequest request(int builds, boolean expand, Map<String, Integer> inv, MaterialRequirement... reqs) {
        return new CalculationRequest(Edition.JAVA, V121, builds, List.of(reqs), inv, expand);
    }

    private static MaterialLine line(CalculationResult r, String id) {
        return r.lines().stream().filter(l -> l.materialId().equals(id)).findFirst().orElseThrow();
    }

    @Test @DisplayName("quantidade básica")
    void basicQuantity() {
        var r = calculator().calculate(request(1, false, Map.of(), new MaterialRequirement("cobblestone", 10)));
        assertEquals(10, line(r, "cobblestone").required());
        assertEquals(10, line(r, "cobblestone").missing());
        assertTrue(r.exact());
    }

    @Test @DisplayName("consolida materiais repetidos entre etapas")
    void consolidation() {
        var r = calculator().calculate(request(1, false, Map.of(),
                new MaterialRequirement("cobblestone", 10), new MaterialRequirement("cobblestone", 5)));
        assertEquals(1, r.lines().size());
        assertEquals(15, line(r, "cobblestone").required());
    }

    @Test @DisplayName("múltiplas construções")
    void multipleBuilds() {
        var r = calculator().calculate(request(3, false, Map.of(), new MaterialRequirement("cobblestone", 15)));
        assertEquals(45, line(r, "cobblestone").required());
    }

    @Test @DisplayName("inventário maior que o necessário")
    void inventoryLargerThanNeeded() {
        var r = calculator().calculate(request(1, false, Map.of("cobblestone", 100), new MaterialRequirement("cobblestone", 15)));
        assertEquals(0, line(r, "cobblestone").missing());
        assertEquals(15, line(r, "cobblestone").fromInventory());
    }

    @Test @DisplayName("inventário parcialmente suficiente")
    void partialInventory() {
        var r = calculator().calculate(request(1, false, Map.of("cobblestone", 6), new MaterialRequirement("cobblestone", 15)));
        assertEquals(9, line(r, "cobblestone").missing());
    }

    @Test @DisplayName("inventário vazio")
    void emptyInventory() {
        var r = calculator().calculate(request(2, false, null, new MaterialRequirement("cobblestone", 7)));
        assertEquals(0, line(r, "cobblestone").fromInventory());
        assertEquals(14, line(r, "cobblestone").missing());
    }

    @Test @DisplayName("stacks completas e unidades restantes")
    void stacks() {
        var r = calculator().calculate(request(1, false, Map.of(), new MaterialRequirement("cobblestone", 130)));
        assertEquals(2, line(r, "cobblestone").requiredStacks().fullStacks());
        assertEquals(2, line(r, "cobblestone").requiredStacks().remainder());
    }

    @Test @DisplayName("receita com excedente: 5 planks => 2 operações, 8 produzidas, 3 de excedente")
    void recipeSurplus() {
        var r = calculator().calculate(request(1, true, Map.of(), new MaterialRequirement("planks", 5)));
        var planks = line(r, "planks");
        assertEquals(2, planks.craftOperations());
        assertEquals(8, planks.craftedUnits());
        assertEquals(3, planks.surplus());
        assertEquals(0, planks.toGather());
        assertEquals(2, line(r, "log").required());
        assertEquals(2, line(r, "log").toGather());
    }

    @Test @DisplayName("receitas encadeadas, sem descontar inventário duas vezes")
    void chainedRecipes() {
        // chest = 8 planks; 5 sticks = 2 ops => 4 planks. Demanda total de planks = 12.
        var r = calculator().calculate(request(1, true, Map.of("planks", 5),
                new MaterialRequirement("chest", 1), new MaterialRequirement("stick", 5)));
        var planks = line(r, "planks");
        assertEquals(12, planks.required());
        assertEquals(5, planks.fromInventory());
        assertEquals(7, planks.missing());
        assertEquals(2, planks.craftOperations());
        assertEquals(1, planks.surplus());
        assertEquals(2, line(r, "log").required());
        assertEquals(3, line(r, "stick").surplus());
    }

    @Test @DisplayName("receita ausente gera aviso e resultado não exato")
    void missingRecipe() {
        var r = calculator().calculate(request(1, true, Map.of(), new MaterialRequirement("lantern", 2)));
        assertFalse(r.exact());
        assertEquals(IssueType.RECIPE_NOT_REGISTERED, r.issues().get(0).type());
        assertEquals(2, line(r, "lantern").toGather());
    }

    @Test @DisplayName("ciclo de dependências")
    void cycle() {
        var cyclic = List.of(
                recipe("r-a", "a", 1, BOTH, null, new Recipe.Ingredient("b", 1)),
                recipe("r-b", "b", 1, BOTH, null, new Recipe.Ingredient("a", 1)));
        var calc = new MaterialCalculator(MATERIALS, cyclic);
        var ex = assertThrows(RecipeCycleException.class,
                () -> calc.calculate(request(1, true, Map.of(), new MaterialRequirement("a", 1))));
        assertEquals(List.of("a", "b"), ex.materialIds());
    }

    @Test @DisplayName("compatibilidade incorreta de edição e de versão")
    void incompatibility() {
        var javaOnly = List.of(recipe("r-planks", "planks", 4, Set.of(Edition.JAVA), null, new Recipe.Ingredient("log", 1)));
        var calc = new MaterialCalculator(MATERIALS, javaOnly);
        var bedrock = new CalculationRequest(Edition.BEDROCK, V121, 1, List.of(new MaterialRequirement("planks", 4)), Map.of(), true);
        assertEquals(IssueType.RECIPE_INCOMPATIBLE, calc.calculate(bedrock).issues().get(0).type());

        var newer = List.of(recipe("r-planks", "planks", 4, BOTH, GameVersion.parse("1.22"), new Recipe.Ingredient("log", 1)));
        var calc2 = new MaterialCalculator(MATERIALS, newer);
        var old = request(1, true, Map.of(), new MaterialRequirement("planks", 4));
        assertEquals(IssueType.RECIPE_INCOMPATIBLE, calc2.calculate(old).issues().get(0).type());
    }

    @Test @DisplayName("quantidade zero")
    void zeroQuantity() {
        assertThrows(InvalidCalculationException.class,
                () -> calculator().calculate(request(1, false, Map.of(), new MaterialRequirement("cobblestone", 0))));
        assertThrows(InvalidCalculationException.class,
                () -> calculator().calculate(request(0, false, Map.of(), new MaterialRequirement("cobblestone", 1))));
    }

    @Test @DisplayName("quantidade negativa")
    void negativeQuantity() {
        assertThrows(InvalidCalculationException.class,
                () -> calculator().calculate(request(-1, false, Map.of(), new MaterialRequirement("cobblestone", 1))));
        assertThrows(InvalidCalculationException.class,
                () -> calculator().calculate(request(1, false, Map.of("cobblestone", -5), new MaterialRequirement("cobblestone", 1))));
        assertThrows(InvalidCalculationException.class,
                () -> calculator().calculate(request(1, false, Map.of(), new MaterialRequirement("cobblestone", -3))));
    }

    @Test @DisplayName("valores fora dos limites e materiais desconhecidos")
    void outOfLimits() {
        assertThrows(InvalidCalculationException.class,
                () -> calculator().calculate(request(1001, false, Map.of(), new MaterialRequirement("cobblestone", 1))));
        assertThrows(InvalidCalculationException.class,
                () -> calculator().calculate(request(1, false, Map.of(), new MaterialRequirement("cobblestone", 10_000_001))));
        assertThrows(InvalidCalculationException.class,
                () -> calculator().calculate(request(1, false, Map.of(), new MaterialRequirement("unobtainium", 1))));
    }
}
