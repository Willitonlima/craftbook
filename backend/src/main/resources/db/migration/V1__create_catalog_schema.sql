CREATE TABLE materials (
    id VARCHAR(100) PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(80) NOT NULL,
    stack_size INTEGER NOT NULL CHECK (stack_size BETWEEN 1 AND 64),
    craftable BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE recipes (
    id VARCHAR(100) PRIMARY KEY,
    output_material_id VARCHAR(100) NOT NULL REFERENCES materials (id),
    output_quantity INTEGER NOT NULL CHECK (output_quantity > 0),
    min_version VARCHAR(19),
    max_version VARCHAR(19)
);

CREATE TABLE recipe_editions (
    recipe_id VARCHAR(100) NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    edition VARCHAR(20) NOT NULL CHECK (edition IN ('JAVA', 'BEDROCK')),
    PRIMARY KEY (recipe_id, edition)
);

CREATE TABLE recipe_ingredients (
    recipe_id VARCHAR(100) NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    material_id VARCHAR(100) NOT NULL REFERENCES materials (id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (recipe_id, material_id)
);

CREATE INDEX idx_recipes_output_material ON recipes (output_material_id);
CREATE INDEX idx_recipe_ingredients_material ON recipe_ingredients (material_id);
