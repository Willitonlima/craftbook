INSERT INTO materials (id, name, category, stack_size, craftable) VALUES
    ('oak_log', 'Tronco de carvalho', 'Madeira', 64, FALSE),
    ('oak_planks', 'Tábuas de carvalho', 'Madeira', 64, TRUE),
    ('stick', 'Graveto', 'Madeira', 64, TRUE),
    ('crafting_table', 'Bancada de trabalho', 'Estações', 64, TRUE),
    ('chest', 'Baú', 'Armazenamento', 64, TRUE),
    ('cobblestone', 'Pedregulho', 'Mineração', 64, FALSE),
    ('furnace', 'Fornalha', 'Estações', 64, TRUE),
    ('coal', 'Carvão', 'Mineração', 64, FALSE),
    ('torch', 'Tocha', 'Iluminação', 64, TRUE),
    ('iron_ingot', 'Barra de ferro', 'Mineração', 64, FALSE),
    ('iron_pickaxe', 'Picareta de ferro', 'Ferramentas', 1, TRUE);

INSERT INTO recipes (id, output_material_id, output_quantity, min_version) VALUES
    ('oak_planks_from_log', 'oak_planks', 4, '1.0'),
    ('sticks_from_planks', 'stick', 4, '1.0'),
    ('crafting_table_from_planks', 'crafting_table', 1, '1.0'),
    ('chest_from_planks', 'chest', 1, '1.0'),
    ('furnace_from_cobblestone', 'furnace', 1, '1.0'),
    ('torches_from_coal', 'torch', 4, '1.0'),
    ('iron_pickaxe_from_ingots', 'iron_pickaxe', 1, '1.0');

INSERT INTO recipe_editions (recipe_id, edition)
SELECT recipe.id, edition.edition
FROM recipes recipe
CROSS JOIN (VALUES ('JAVA'), ('BEDROCK')) AS edition(edition);

INSERT INTO recipe_ingredients (recipe_id, material_id, quantity) VALUES
    ('oak_planks_from_log', 'oak_log', 1),
    ('sticks_from_planks', 'oak_planks', 2),
    ('crafting_table_from_planks', 'oak_planks', 4),
    ('chest_from_planks', 'oak_planks', 8),
    ('furnace_from_cobblestone', 'cobblestone', 8),
    ('torches_from_coal', 'coal', 1),
    ('torches_from_coal', 'stick', 1),
    ('iron_pickaxe_from_ingots', 'iron_ingot', 3),
    ('iron_pickaxe_from_ingots', 'stick', 2);
