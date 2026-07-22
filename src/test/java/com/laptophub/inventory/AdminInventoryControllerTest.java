package com.laptophub.inventory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.auth.dto.LoginRequest;
import com.laptophub.catalog.entity.Brand;
import com.laptophub.catalog.entity.Category;
import com.laptophub.catalog.entity.Product;
import com.laptophub.catalog.entity.ProductVariant;
import com.laptophub.catalog.repository.BrandRepository;
import com.laptophub.catalog.repository.CategoryRepository;
import com.laptophub.catalog.repository.ProductRepository;
import com.laptophub.catalog.repository.ProductVariantRepository;
import com.laptophub.inventory.dto.InventoryAdjustRequest;
import com.laptophub.user.entity.User;
import com.laptophub.user.entity.UserRole;
import com.laptophub.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminInventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String registerAdminAndLogin(String email) throws Exception {
        userRepository.saveAndFlush(User.create(email, passwordEncoder.encode("Sup3rSecret!"), "Admin", null,
                UserRole.ADMIN));
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "Sup3rSecret!"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private String registerCustomerAndLogin(String email) throws Exception {
        userRepository.saveAndFlush(User.create(email, passwordEncoder.encode("Sup3rSecret!"), "Cust", null,
                UserRole.CUSTOMER));
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "Sup3rSecret!"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private Long newVariantId(String slug) {
        Long categoryId = categoryRepository.saveAndFlush(Category.create("Cat " + slug, "cat-" + slug, null)).getId();
        Long brandId = brandRepository.saveAndFlush(Brand.create("Brand " + slug, "brand-" + slug, null, null)).getId();
        Long productId = productRepository
                .saveAndFlush(Product.create(categoryId, brandId, "Product " + slug, slug, null, null)).getId();
        return productVariantRepository
                .saveAndFlush(ProductVariant.create(productId, "SKU-" + slug, null, BigDecimal.TEN, null, null,
                        null, null))
                .getId();
    }

    @Test
    void getBalance_returnsZero_whenNoBalanceYet() throws Exception {
        String adminToken = registerAdminAndLogin("inv-admin-balance-zero@example.com");
        Long variantId = newVariantId("inv-balance-zero");

        mockMvc.perform(get("/admin/inventory/" + variantId + "/balance")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.onHandQuantity").value(0))
                .andExpect(jsonPath("$.data.reservedQuantity").value(0))
                .andExpect(jsonPath("$.data.availableQuantity").value(0));
    }

    @Test
    void getBalance_missingVariant_returns404() throws Exception {
        String adminToken = registerAdminAndLogin("inv-admin-balance-missing@example.com");

        mockMvc.perform(get("/admin/inventory/999999/balance").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void adjust_increasesOnHand_andReturnsUpdatedBalance() throws Exception {
        String adminToken = registerAdminAndLogin("inv-admin-adjust-up@example.com");
        Long variantId = newVariantId("inv-adjust-up");

        mockMvc.perform(post("/admin/inventory/" + variantId + "/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InventoryAdjustRequest(10, "Nhap thu cong"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.onHandQuantity").value(10))
                .andExpect(jsonPath("$.data.availableQuantity").value(10));
    }

    @Test
    void adjust_missingReason_returns400() throws Exception {
        String adminToken = registerAdminAndLogin("inv-admin-adjust-noreason@example.com");
        Long variantId = newVariantId("inv-adjust-noreason");

        mockMvc.perform(post("/admin/inventory/" + variantId + "/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InventoryAdjustRequest(10, " "))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adjust_belowZero_returnsInsufficientStock() throws Exception {
        String adminToken = registerAdminAndLogin("inv-admin-adjust-neg@example.com");
        Long variantId = newVariantId("inv-adjust-neg");

        mockMvc.perform(post("/admin/inventory/" + variantId + "/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new InventoryAdjustRequest(-5, "Hang hong"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"));
    }

    @Test
    void listMovements_returnsRecordedMovementsAfterAdjust() throws Exception {
        String adminToken = registerAdminAndLogin("inv-admin-movements@example.com");
        Long variantId = newVariantId("inv-movements");
        mockMvc.perform(post("/admin/inventory/" + variantId + "/adjustments")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new InventoryAdjustRequest(7, "Kiem ke"))));

        mockMvc.perform(get("/admin/inventory/" + variantId + "/movements")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].type").value("ADJUSTMENT_IN"))
                .andExpect(jsonPath("$.data.content[0].quantity").value(7));
    }

    @Test
    void asCustomer_returns403() throws Exception {
        String customerToken = registerCustomerAndLogin("inv-customer@example.com");
        Long variantId = newVariantId("inv-customer-forbidden");

        mockMvc.perform(get("/admin/inventory/" + variantId + "/balance")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/admin/inventory/1/balance")).andExpect(status().isUnauthorized());
    }
}
