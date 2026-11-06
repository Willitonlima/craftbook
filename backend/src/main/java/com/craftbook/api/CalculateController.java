package com.craftbook.api;

import com.craftbook.calculator.application.CalculationService;
import com.craftbook.calculator.domain.CalculationRequest;
import com.craftbook.calculator.domain.CalculationResult;
import com.craftbook.calculator.domain.Edition;
import com.craftbook.calculator.domain.GameVersion;
import com.craftbook.calculator.domain.MaterialRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calculator")
public class CalculateController {

    private final CalculationService calculation;

    public CalculateController(CalculationService calculation) {
        this.calculation = calculation;
    }

    @PostMapping
    public CalculationResult calculate(@Valid @RequestBody CalculationBody body) {
        return calculation.calculate(new CalculationRequest(
                body.edition(),
                GameVersion.parse(body.version()),
                body.buildQuantity(),
                body.requirements().stream()
                        .map(requirement -> new MaterialRequirement(requirement.materialId(), requirement.quantity()))
                        .toList(),
                body.inventory(),
                body.expandRecipes()));
    }

    public record CalculationBody(
            @NotNull Edition edition,
            @NotBlank @Pattern(regexp = "\\d{1,4}(\\.\\d{1,4}){1,3}") String version,
            @Min(1) @Max(1_000) int buildQuantity,
            @NotEmpty @Size(max = 500) List<@Valid RequirementBody> requirements,
            Map<@NotBlank String, @Min(0) @Max(10_000_000) Integer> inventory,
            boolean expandRecipes) { }

    public record RequirementBody(
            @NotBlank String materialId,
            @Min(1) @Max(10_000_000) int quantity) { }
}
