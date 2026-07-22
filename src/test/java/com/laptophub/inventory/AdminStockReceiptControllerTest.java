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
import com.laptophub.inventory.dto.StockReceiptCreateRequest;
import com.laptophub.inventory.dto.StockReceiptItemRequest;
import com.laptophub.inventory.dto.StockReceiptItemsReplaceRequest;
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
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminStockReceiptControllerTest {

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

    private long createReceipt(String adminToken, String code, Long variantId, int quantity) throws Exception {
        MvcResult result = mockMvc.perform(post("/admin/stock-receipts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StockReceiptCreateRequest(code, "Nhap tu NCC",
                                List.of(new StockReceiptItemRequest(variantId, quantity))))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    @Test
    void create_savesReceiptWithItems_returns201() throws Exception {
        String adminToken = registerAdminAndLogin("receipt-admin-create@example.com");
        Long variantId = newVariantId("receipt-create");

        mockMvc.perform(post("/admin/stock-receipts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StockReceiptCreateRequest("PN-CREATE-1",
                                "Nhap tu NCC A", List.of(new StockReceiptItemRequest(variantId, 5))))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("PN-CREATE-1"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(5));
    }

    @Test
    void create_duplicateCode_returns409() throws Exception {
        String adminToken = registerAdminAndLogin("receipt-admin-dup@example.com");
        Long variantId = newVariantId("receipt-dup");
        createReceipt(adminToken, "PN-DUP-1", variantId, 5);

        mockMvc.perform(post("/admin/stock-receipts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StockReceiptCreateRequest("PN-DUP-1", null,
                                List.of(new StockReceiptItemRequest(variantId, 3))))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_CONFLICT"));
    }

    @Test
    void create_missingVariant_returns404() throws Exception {
        String adminToken = registerAdminAndLogin("receipt-admin-missingvariant@example.com");

        mockMvc.perform(post("/admin/stock-receipts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StockReceiptCreateRequest("PN-MISSING-1", null,
                                List.of(new StockReceiptItemRequest(999999L, 3))))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void getOne_returnsDetailWithItems() throws Exception {
        String adminToken = registerAdminAndLogin("receipt-admin-getone@example.com");
        Long variantId = newVariantId("receipt-getone");
        long receiptId = createReceipt(adminToken, "PN-GETONE-1", variantId, 8);

        mockMvc.perform(get("/admin/stock-receipts/" + receiptId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("PN-GETONE-1"))
                .andExpect(jsonPath("$.data.items[0].sku").value("SKU-receipt-getone"));
    }

    @Test
    void getOne_missing_returns404() throws Exception {
        String adminToken = registerAdminAndLogin("receipt-admin-getone-missing@example.com");

        mockMvc.perform(get("/admin/stock-receipts/999999").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void replaceItems_whenDraft_replacesItems() throws Exception {
        String adminToken = registerAdminAndLogin("receipt-admin-replace@example.com");
        Long variantId = newVariantId("receipt-replace-1");
        Long otherVariantId = newVariantId("receipt-replace-2");
        long receiptId = createReceipt(adminToken, "PN-REPLACE-1", variantId, 5);

        mockMvc.perform(put("/admin/stock-receipts/" + receiptId + "/items")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new StockReceiptItemsReplaceRequest(List.of(
                                        new StockReceiptItemRequest(otherVariantId, 12))))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].quantity").value(12));
    }

    @Test
    void confirm_increasesBalance_andTransitionsStatus() throws Exception {
        String adminToken = registerAdminAndLogin("receipt-admin-confirm@example.com");
        Long variantId = newVariantId("receipt-confirm");
        long receiptId = createReceipt(adminToken, "PN-CONFIRM-1", variantId, 15);

        mockMvc.perform(post("/admin/stock-receipts/" + receiptId + "/confirm")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        mockMvc.perform(get("/admin/inventory/" + variantId + "/balance")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.onHandQuantity").value(15));
    }

    @Test
    void confirm_alreadyConfirmed_returns400() throws Exception {
        String adminToken = registerAdminAndLogin("receipt-admin-doubleconfirm@example.com");
        Long variantId = newVariantId("receipt-doubleconfirm");
        long receiptId = createReceipt(adminToken, "PN-DOUBLECONFIRM-1", variantId, 5);
        mockMvc.perform(post("/admin/stock-receipts/" + receiptId + "/confirm")
                .header("Authorization", "Bearer " + adminToken));

        mockMvc.perform(post("/admin/stock-receipts/" + receiptId + "/confirm")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_STOCK_RECEIPT_STATUS"));
    }

    @Test
    void cancel_draft_transitionsToCancelled() throws Exception {
        String adminToken = registerAdminAndLogin("receipt-admin-cancel@example.com");
        Long variantId = newVariantId("receipt-cancel");
        long receiptId = createReceipt(adminToken, "PN-CANCEL-1", variantId, 5);

        mockMvc.perform(post("/admin/stock-receipts/" + receiptId + "/cancel")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void list_returnsCreatedReceipt() throws Exception {
        String adminToken = registerAdminAndLogin("receipt-admin-list@example.com");
        Long variantId = newVariantId("receipt-list");
        createReceipt(adminToken, "PN-LIST-1", variantId, 5);

        mockMvc.perform(get("/admin/stock-receipts").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void asCustomer_returns403() throws Exception {
        String customerToken = registerCustomerAndLogin("receipt-customer@example.com");

        mockMvc.perform(get("/admin/stock-receipts").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/admin/stock-receipts")).andExpect(status().isUnauthorized());
    }
}
