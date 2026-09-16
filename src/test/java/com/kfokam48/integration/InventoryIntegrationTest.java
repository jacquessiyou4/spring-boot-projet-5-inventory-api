package com.kfokam48.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfokam48.dto.CreateProductDTO;
import com.kfokam48.dto.StockMovementDTO;
import com.kfokam48.repository.ProductRepository;
import com.kfokam48.repository.StockAlertRepository;
import com.kfokam48.repository.MovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockAlertRepository stockAlertRepository;

    @Autowired
    private MovementRepository movementRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        movementRepository.deleteAll();
        stockAlertRepository.deleteAll();
        productRepository.deleteAll();
    }

    private long createProduct(String sku, String name, int stock, int minStock) throws Exception {
        CreateProductDTO dto = new CreateProductDTO();
        dto.setSku(sku);
        dto.setName(name);
        dto.setCategory("Electronics");
        dto.setPrice(BigDecimal.valueOf(99.99));
        dto.setInitialStock(stock);
        dto.setMinStockLevel(minStock);

        MvcResult result = mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void fullProductAndStockFlow() throws Exception {
        // CREATE PRODUCT
        long productId = createProduct("PHONE001", "Smartphone", 50, 10);

        // GET ALL PRODUCTS
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        // GET PRODUCT BY ID
        mockMvc.perform(get("/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Smartphone")));

        // STOCK IN
        StockMovementDTO stockIn = new StockMovementDTO(30, "Restock from supplier", "PO-001");
        mockMvc.perform(post("/products/" + productId + "/stock/in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stockIn)))
                .andExpect(status().isOk());

        // VERIFY STOCK
        mockMvc.perform(get("/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStock", is(80)));

        // STOCK OUT
        StockMovementDTO stockOut = new StockMovementDTO(5, "Customer order", "ORD-001");
        mockMvc.perform(post("/products/" + productId + "/stock/out")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stockOut)))
                .andExpect(status().isOk());

        // VERIFY STOCK AFTER OUT
        mockMvc.perform(get("/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStock", is(75)));

        // GET MOVEMENTS
        mockMvc.perform(get("/products/" + productId + "/stock/movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        // GET ALERTS (none yet)
        mockMvc.perform(get("/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        // UPDATE PRODUCT
        mockMvc.perform(put("/products/" + productId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\": \"PHONE001\", \"name\": \"Smartphone Pro\", \"category\": \"Electronics\", \"price\": 999.99}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Smartphone Pro")));

        // DELETE PRODUCT
        mockMvc.perform(delete("/products/" + productId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/products/" + productId))
                .andExpect(status().isNotFound());
    }

    @Test
    void createProduct_withInvalidData_shouldReturn400() throws Exception {
        mockMvc.perform(post("/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\": \"\", \"name\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void stockOut_withInsufficientStock_shouldReturn400() throws Exception {
        long productId = createProduct("PROD001", "Product", 5, 1);

        StockMovementDTO out = new StockMovementDTO(10, "Too much", null);
        mockMvc.perform(post("/products/" + productId + "/stock/out")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(out)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void stockOut_triggeringLowStock_shouldCreateAlert() throws Exception {
        long productId = createProduct("PROD002", "Low Stock Product", 15, 10);

        StockMovementDTO out = new StockMovementDTO(10, "Sale", null);
        mockMvc.perform(post("/products/" + productId + "/stock/out")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(out)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/alerts/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void resolveAlert_shouldWork() throws Exception {
        long productId = createProduct("PROD003", "Alert Product", 12, 10);

        StockMovementDTO out = new StockMovementDTO(5, "Sale", null);
        mockMvc.perform(post("/products/" + productId + "/stock/out")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(out)))
                .andExpect(status().isOk());

        // Get alert ID dynamically
        MvcResult alertsResult = mockMvc.perform(get("/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andReturn();

        long alertId = objectMapper.readTree(alertsResult.getResponse().getContentAsString())
                .get(0).get("id").asLong();

        // Resolve the alert
        mockMvc.perform(put("/alerts/" + alertId + "/resolve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resolved", is(true)));

        // No more active alerts
        mockMvc.perform(get("/alerts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getLowStockProducts_shouldReturnFiltered() throws Exception {
        createProduct("LOW001", "Low Product", 5, 10);
        createProduct("OK001", "OK Product", 100, 10);

        mockMvc.perform(get("/products/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].sku", is("LOW001")));
    }

    /**
     * Le cahier des charges demande explicitement de pouvoir modifier la
     * quantité d'un produit : l'écart est tracé comme mouvement ADJUSTMENT.
     */
    @Test
    void updateProduct_shouldChangeQuantityAndRecordAdjustment() throws Exception {
        long id = createProduct("SKU-ADJ-1", "Produit ajustable", 20, 5);

        mockMvc.perform(put("/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\": \"SKU-ADJ-1\", \"name\": \"Produit ajustable\", " +
                                 "\"category\": \"Tech\", \"price\": 19.99, \"initialStock\": 7, " +
                                 "\"minStockLevel\": 5, \"unit\": \"UNIT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStock", is(7)));

        mockMvc.perform(get("/products/" + id + "/stock/movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].movementType", is("ADJUSTMENT")))
                .andExpect(jsonPath("$[0].quantityBefore", is(20)))
                .andExpect(jsonPath("$[0].quantityAfter", is(7)));
    }
}
