package com.craftbook.calculator.application;

import com.craftbook.calculator.domain.CalculationRequest;
import com.craftbook.calculator.domain.CalculationResult;
import com.craftbook.catalog.CatalogService;
import org.springframework.stereotype.Service;

@Service
public class CalculationService {

    private final CatalogService catalog;

    public CalculationService(CatalogService catalog) {
        this.catalog = catalog;
    }

    public CalculationResult calculate(CalculationRequest request) {
        return new MaterialCalculator(catalog.loadDomainMaterials(), catalog.loadDomainRecipes()).calculate(request);
    }
}
