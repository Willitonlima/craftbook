package com.craftbook.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.craftbook.calculator.application.CalculationService;
import com.craftbook.calculator.domain.CalculationRequest;
import com.craftbook.calculator.domain.CalculationResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CalculateController.class)
class CalculateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CalculationService calculation;

    @Test
    void calculatesAValidatedRequest() throws Exception {
        when(calculation.calculate(any(CalculationRequest.class)))
                .thenReturn(new CalculationResult(List.of(), List.of(), true));

        mockMvc.perform(post("/api/calculator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "edition": "JAVA",
                                  "version": "1.21.4",
                                  "buildQuantity": 2,
                                  "requirements": [{"materialId": "chest", "quantity": 3}],
                                  "inventory": {"chest": 1},
                                  "expandRecipes": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exact").value(true));

        verify(calculation).calculate(any(CalculationRequest.class));
    }

    @Test
    void rejectsMalformedGameVersion() throws Exception {
        mockMvc.perform(post("/api/calculator")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "edition": "JAVA",
                                  "version": "latest",
                                  "buildQuantity": 1,
                                  "requirements": [{"materialId": "chest", "quantity": 1}],
                                  "expandRecipes": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0]").exists());
    }
}
