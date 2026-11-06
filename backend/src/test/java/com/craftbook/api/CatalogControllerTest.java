package com.craftbook.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.craftbook.catalog.CatalogService;
import com.craftbook.catalog.CatalogService.MaterialSummary;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CatalogController.class)
class CatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogService catalog;

    @Test
    void filtersMaterialsBySearchAndCategory() throws Exception {
        when(catalog.findMaterials("baú", "Armazenamento"))
                .thenReturn(List.of(new MaterialSummary("chest", "Baú", "Armazenamento", 64, true)));

        mockMvc.perform(get("/api/catalog/materials")
                        .param("query", "baú")
                        .param("category", "Armazenamento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("chest"))
                .andExpect(jsonPath("$[0].stackSize").value(64));
    }
}
