package com.laptophub.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.auth.dto.LoginRequest;
import com.laptophub.catalog.dto.ProductCreateRequest;
import com.laptophub.catalog.dto.ProductImageCreateRequest;
import com.laptophub.catalog.dto.ProductImageReorderRequest;
import com.laptophub.catalog.dto.ProductUpdateRequest;
import com.laptophub.catalog.dto.ProductVariantCreateRequest;
import com.laptophub.catalog.dto.ProductVariantUpdateRequest;
import com.laptophub.catalog.entity.Brand;
import com.laptophub.catalog.entity.Category;
import com.laptophub.catalog.repository.BrandRepository;
import com.laptophub.catalog.repository.CategoryRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminProductControllerTest {

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

    private Long newActiveCategory(String slug) {
        return categoryRepository.saveAndFlush(Category.create("Cat " + slug, slug, null)).getId();
    }

    private Long newActiveBrand(String slug) {
        return brandRepository.saveAndFlush(Brand.create("Brand " + slug, slug, null, null)).getId();
    }

    private Long newInactiveCategory(String slug) {
        Category category = Category.create("Cat " + slug, slug, null);
        category.deactivate();
        return categoryRepository.saveAndFlush(category).getId();
    }

    private long newProductId(String adminToken, String slug) throws Exception {
        Long categoryId = newActiveCategory("cat-" + slug);
        Long brandId = newActiveBrand("brand-" + slug);
        MvcResult result = mockMvc.perform(post("/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProductCreateRequest(categoryId, brandId, "Product " + slug, slug, null, null))))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    @Test
    void create_returns201_withCategoryAndBrandNames() throws Exception {
        String adminToken = registerAdminAndLogin("prod-admin-create@example.com");
        Long categoryId = newActiveCategory("cat-prod-create");
        Long brandId = newActiveBrand("brand-prod-create");

        mockMvc.perform(post("/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProductCreateRequest(categoryId, brandId, "Laptop Gaming ABC", null, "Ngan", "Dai"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.slug").value("laptop-gaming-abc"))
                .andExpect(jsonPath("$.data.categoryName").value("Cat cat-prod-create"))
                .andExpect(jsonPath("$.data.brandName").value("Brand brand-prod-create"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void create_withInactiveCategory_returns400() throws Exception {
        String adminToken = registerAdminAndLogin("prod-admin-inactive@example.com");
        Long categoryId = newInactiveCategory("cat-prod-inactive");
        Long brandId = newActiveBrand("brand-prod-inactive");

        mockMvc.perform(post("/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProductCreateRequest(categoryId, brandId, "Laptop", null, null, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void create_duplicateSlug_returns409() throws Exception {
        String adminToken = registerAdminAndLogin("prod-admin-dup@example.com");
        Long categoryId = newActiveCategory("cat-prod-dup");
        Long brandId = newActiveBrand("brand-prod-dup");
        mockMvc.perform(post("/admin/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new ProductCreateRequest(categoryId, brandId, "Laptop", "laptop-dup-slug", null, null))));

        mockMvc.perform(post("/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProductCreateRequest(categoryId, brandId, "Laptop Khac", "laptop-dup-slug", null, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_CONFLICT"));
    }

    @Test
    void update_changesFields() throws Exception {
        String adminToken = registerAdminAndLogin("prod-admin-update@example.com");
        Long categoryId = newActiveCategory("cat-prod-update");
        Long brandId = newActiveBrand("brand-prod-update");
        MvcResult createResult = mockMvc.perform(post("/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProductCreateRequest(categoryId, brandId, "Laptop", null, null, null))))
                .andReturn();
        long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        mockMvc.perform(put("/admin/products/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProductUpdateRequest(categoryId, brandId, "Laptop Moi", null, "Ngan moi", "Dai moi"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Laptop Moi"))
                .andExpect(jsonPath("$.data.slug").value("laptop-moi"));
    }

    @Test
    void getOne_missing_returns404() throws Exception {
        String adminToken = registerAdminAndLogin("prod-admin-missing@example.com");

        mockMvc.perform(get("/admin/products/999999").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void activateAndDeactivate_toggleStatus() throws Exception {
        String adminToken = registerAdminAndLogin("prod-admin-toggle@example.com");
        Long categoryId = newActiveCategory("cat-prod-toggle");
        Long brandId = newActiveBrand("brand-prod-toggle");
        MvcResult createResult = mockMvc.perform(post("/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProductCreateRequest(categoryId, brandId, "Laptop", null, null, null))))
                .andReturn();
        long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        mockMvc.perform(post("/admin/products/" + id + "/deactivate").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        mockMvc.perform(post("/admin/products/" + id + "/activate").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void list_returnsPagedProducts_withEnrichedNames() throws Exception {
        String adminToken = registerAdminAndLogin("prod-admin-list@example.com");
        Long categoryId = newActiveCategory("cat-prod-list");
        Long brandId = newActiveBrand("brand-prod-list");
        mockMvc.perform(post("/admin/products")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(
                        new ProductCreateRequest(categoryId, brandId, "Laptop List Test", null, null, null))));

        mockMvc.perform(get("/admin/products?categoryId=" + categoryId + "&page=0&size=20")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].categoryName").value("Cat cat-prod-list"))
                .andExpect(jsonPath("$.data.content[0].brandName").value("Brand brand-prod-list"));
    }

    @Test
    void addVariant_returns201() throws Exception {
        String adminToken = registerAdminAndLogin("prod-admin-variant-add@example.com");
        long productId = newProductId(adminToken, "variant-add");

        mockMvc.perform(post("/admin/products/" + productId + "/variants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductVariantCreateRequest(
                                "SKU-ADD-1", "16GB/512GB", new BigDecimal("999.00"), 16, 512, "SSD", "Black"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sku").value("SKU-ADD-1"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void addVariant_duplicateSku_returns409() throws Exception {
        String adminToken = registerAdminAndLogin("prod-admin-variant-dup@example.com");
        long productId = newProductId(adminToken, "variant-dup");
        mockMvc.perform(post("/admin/products/" + productId + "/variants")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ProductVariantCreateRequest(
                        "SKU-DUP-1", null, BigDecimal.TEN, null, null, null, null))));

        mockMvc.perform(post("/admin/products/" + productId + "/variants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductVariantCreateRequest(
                                "SKU-DUP-1", null, BigDecimal.ONE, null, null, null, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_CONFLICT"));
    }

    @Test
    void updateVariant_changesFields() throws Exception {
        String adminToken = registerAdminAndLogin("prod-admin-variant-update@example.com");
        long productId = newProductId(adminToken, "variant-update");
        MvcResult createResult = mockMvc.perform(post("/admin/products/" + productId + "/variants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductVariantCreateRequest(
                                "SKU-UPD-1", null, BigDecimal.TEN, null, null, null, null))))
                .andReturn();
        long variantId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(put("/admin/products/" + productId + "/variants/" + variantId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductVariantUpdateRequest(
                                "8GB/256GB", new BigDecimal("799.00"), 8, 256, "SSD", "Silver"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.variantName").value("8GB/256GB"))
                .andExpect(jsonPath("$.data.color").value("Silver"));
    }

    @Test
    void variantOfAnotherProduct_returns404() throws Exception {
        String adminToken = registerAdminAndLogin("prod-admin-variant-wrongproduct@example.com");
        long productId = newProductId(adminToken, "variant-owner");
        long otherProductId = newProductId(adminToken, "variant-owner-other");
        MvcResult createResult = mockMvc.perform(post("/admin/products/" + productId + "/variants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductVariantCreateRequest(
                                "SKU-OWNER-1", null, BigDecimal.TEN, null, null, null, null))))
                .andReturn();
        long variantId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(post("/admin/products/" + otherProductId + "/variants/" + variantId + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void activateAndDeactivateVariant_toggleStatus() throws Exception {
        String adminToken = registerAdminAndLogin("prod-admin-variant-toggle@example.com");
        long productId = newProductId(adminToken, "variant-toggle");
        MvcResult createResult = mockMvc.perform(post("/admin/products/" + productId + "/variants")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductVariantCreateRequest(
                                "SKU-TOGGLE-1", null, BigDecimal.TEN, null, null, null, null))))
                .andReturn();
        long variantId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(post("/admin/products/" + productId + "/variants/" + variantId + "/deactivate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        mockMvc.perform(post("/admin/products/" + productId + "/variants/" + variantId + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void addImage_returns201() throws Exception {
        String adminToken = registerAdminAndLogin("prod-admin-image-add@example.com");
        long productId = newProductId(adminToken, "image-add");

        mockMvc.perform(post("/admin/products/" + productId + "/images")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProductImageCreateRequest("https://example.com/a.png", "Anh", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.url").value("https://example.com/a.png"))
                .andExpect(jsonPath("$.data.sortOrder").value(0));
    }

    @Test
    void removeImage_deletesImage() throws Exception {
        String adminToken = registerAdminAndLogin("prod-admin-image-remove@example.com");
        long productId = newProductId(adminToken, "image-remove");
        MvcResult createResult = mockMvc.perform(post("/admin/products/" + productId + "/images")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProductImageCreateRequest("https://example.com/a.png", null, null))))
                .andReturn();
        long imageId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(delete("/admin/products/" + productId + "/images/" + imageId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());
    }

    @Test
    void reorderImages_appliesNewOrder() throws Exception {
        String adminToken = registerAdminAndLogin("prod-admin-image-reorder@example.com");
        long productId = newProductId(adminToken, "image-reorder");
        MvcResult first = mockMvc.perform(post("/admin/products/" + productId + "/images")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProductImageCreateRequest("https://example.com/1.png", null, null))))
                .andReturn();
        long firstId = objectMapper.readTree(first.getResponse().getContentAsString()).path("data").path("id").asLong();
        MvcResult second = mockMvc.perform(post("/admin/products/" + productId + "/images")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProductImageCreateRequest("https://example.com/2.png", null, null))))
                .andReturn();
        long secondId = objectMapper.readTree(second.getResponse().getContentAsString()).path("data").path("id").asLong();

        mockMvc.perform(put("/admin/products/" + productId + "/images/reorder")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ProductImageReorderRequest(List.of(secondId, firstId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(secondId))
                .andExpect(jsonPath("$.data[0].sortOrder").value(0))
                .andExpect(jsonPath("$.data[1].id").value(firstId))
                .andExpect(jsonPath("$.data[1].sortOrder").value(1));
    }

    @Test
    void asCustomer_returns403() throws Exception {
        String customerToken = registerCustomerAndLogin("prod-customer@example.com");

        mockMvc.perform(get("/admin/products").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/admin/products")).andExpect(status().isUnauthorized());
    }
}
