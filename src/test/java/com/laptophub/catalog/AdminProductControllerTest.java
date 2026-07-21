package com.laptophub.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.auth.dto.LoginRequest;
import com.laptophub.catalog.dto.ProductCreateRequest;
import com.laptophub.catalog.dto.ProductUpdateRequest;
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
