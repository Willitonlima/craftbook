package com.craftbook.api;

import com.craftbook.catalog.CatalogService;
import com.craftbook.catalog.CatalogService.MaterialSummary;
import com.craftbook.catalog.CatalogService.RecipeSummary;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/materials")
    public List<MaterialSummary> materials(
            @RequestParam(name = "query", required = false) String query,
            @RequestParam(name = "category", required = false) String category) {
        return catalog.findMaterials(query, category);
    }

    @GetMapping("/recipes")
    public List<RecipeSummary> recipes() {
        return catalog.findRecipes();
    }
}
