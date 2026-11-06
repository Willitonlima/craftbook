package com.craftbook.catalog;

import com.craftbook.calculator.domain.Edition;
import com.craftbook.calculator.domain.GameVersion;
import com.craftbook.calculator.domain.Material;
import com.craftbook.calculator.domain.Recipe;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {

    private final JdbcTemplate jdbc;

    public CatalogService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<MaterialSummary> findMaterials(String query, String category) {
        StringBuilder sql = new StringBuilder("""
                SELECT id, name, category, stack_size, craftable
                FROM materials
                WHERE 1 = 1
                """);
        List<Object> parameters = new ArrayList<>();
        if (query != null && !query.isBlank()) {
            sql.append(" AND (LOWER(name) LIKE LOWER(?) OR LOWER(id) LIKE LOWER(?))");
            String pattern = "%" + query.trim() + "%";
            parameters.add(pattern);
            parameters.add(pattern);
        }
        if (category != null && !category.isBlank()) {
            sql.append(" AND LOWER(category) = LOWER(?)");
            parameters.add(category.trim());
        }
        sql.append(" ORDER BY category, name");
        return jdbc.query(sql.toString(), (rs, row) -> new MaterialSummary(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("category"),
                rs.getInt("stack_size"),
                rs.getBoolean("craftable")), parameters.toArray());
    }

    public List<RecipeSummary> findRecipes() {
        String sql = """
                SELECT r.id AS recipe_id, r.output_material_id, output.name AS output_name,
                       r.output_quantity, r.min_version, r.max_version, edition.edition,
                       ingredient.material_id, ingredient_material.name AS ingredient_name,
                       ingredient.quantity
                FROM recipes r
                JOIN materials output ON output.id = r.output_material_id
                JOIN recipe_editions edition ON edition.recipe_id = r.id
                JOIN recipe_ingredients ingredient ON ingredient.recipe_id = r.id
                JOIN materials ingredient_material ON ingredient_material.id = ingredient.material_id
                ORDER BY r.id, edition.edition, ingredient.material_id
                """;
        Map<String, RecipeBuilder> recipes = new LinkedHashMap<>();
        jdbc.query(sql, rs -> {
            String id = rs.getString("recipe_id");
            RecipeBuilder recipe = recipes.computeIfAbsent(id, key -> new RecipeBuilder(
                    key,
                    rsString(rs, "output_material_id"),
                    rsString(rs, "output_name"),
                    rsInt(rs, "output_quantity"),
                    rsString(rs, "min_version"),
                    rsString(rs, "max_version")));
            recipe.editions.add(rs.getString("edition"));
            recipe.ingredients.putIfAbsent(
                    rs.getString("material_id"),
                    new IngredientSummary(
                            rs.getString("material_id"),
                            rs.getString("ingredient_name"),
                            rs.getInt("quantity")));
        });
        return recipes.values().stream().map(RecipeBuilder::toSummary).toList();
    }

    public List<Material> loadDomainMaterials() {
        return findMaterials(null, null).stream()
                .map(material -> new Material(
                        material.id(), material.name(), material.category(), material.stackSize(), material.craftable()))
                .toList();
    }

    public List<Recipe> loadDomainRecipes() {
        return findRecipes().stream()
                .map(recipe -> new Recipe(
                        recipe.id(),
                        recipe.outputMaterialId(),
                        recipe.outputQuantity(),
                        recipe.ingredients().stream()
                                .map(ingredient -> new Recipe.Ingredient(ingredient.materialId(), ingredient.quantity()))
                                .toList(),
                        recipe.editions().stream().map(Edition::valueOf).collect(java.util.stream.Collectors.toSet()),
                        recipe.minVersion() == null ? null : GameVersion.parse(recipe.minVersion()),
                        recipe.maxVersion() == null ? null : GameVersion.parse(recipe.maxVersion())))
                .toList();
    }

    private static String rsString(java.sql.ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (java.sql.SQLException exception) {
            throw new org.springframework.jdbc.UncategorizedSQLException("catalog query", null, exception);
        }
    }

    private static int rsInt(java.sql.ResultSet rs, String column) {
        try {
            return rs.getInt(column);
        } catch (java.sql.SQLException exception) {
            throw new org.springframework.jdbc.UncategorizedSQLException("catalog query", null, exception);
        }
    }

    public record MaterialSummary(String id, String name, String category, int stackSize, boolean craftable) { }

    public record IngredientSummary(String materialId, String name, int quantity) { }

    public record RecipeSummary(
            String id,
            String outputMaterialId,
            String outputName,
            int outputQuantity,
            List<IngredientSummary> ingredients,
            Set<String> editions,
            String minVersion,
            String maxVersion) { }

    private static final class RecipeBuilder {
        private final String id;
        private final String outputMaterialId;
        private final String outputName;
        private final int outputQuantity;
        private final String minVersion;
        private final String maxVersion;
        private final Map<String, IngredientSummary> ingredients = new LinkedHashMap<>();
        private final Set<String> editions = new LinkedHashSet<>();

        private RecipeBuilder(
                String id, String outputMaterialId, String outputName, int outputQuantity,
                String minVersion, String maxVersion) {
            this.id = id;
            this.outputMaterialId = outputMaterialId;
            this.outputName = outputName;
            this.outputQuantity = outputQuantity;
            this.minVersion = minVersion;
            this.maxVersion = maxVersion;
        }

        private RecipeSummary toSummary() {
            return new RecipeSummary(
                    id, outputMaterialId, outputName, outputQuantity,
                    List.copyOf(ingredients.values()), Set.copyOf(editions), minVersion, maxVersion);
        }
    }
}
