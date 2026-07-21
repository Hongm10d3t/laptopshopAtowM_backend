package com.laptophub.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.auth.dto.LoginRequest;
import com.laptophub.catalog.dto.BrandCreateRequest;
import com.laptophub.catalog.dto.BrandUpdateRequest;
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
class AdminBrandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

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

    @Test
    void create_generatesSlug_andReturns201() throws Exception {
        String adminToken = registerAdminAndLogin("brand-admin-create@example.com");

        mockMvc.perform(post("/admin/brands")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new BrandCreateRequest("Asus", null, "Mo ta", "https://example.com/asus.png"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.slug").value("asus"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void create_duplicateSlug_returns409() throws Exception {
        String adminToken = registerAdminAndLogin("brand-admin-dup@example.com");
        mockMvc.perform(post("/admin/brands")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new BrandCreateRequest("Asus", "asus-dup-slug", null, null))));

        mockMvc.perform(post("/admin/brands")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new BrandCreateRequest("Asus Khac", "asus-dup-slug", null, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_CONFLICT"));
    }

    @Test
    void update_changesFields() throws Exception {
        String adminToken = registerAdminAndLogin("brand-admin-update@example.com");
        MvcResult createResult = mockMvc.perform(post("/admin/brands")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BrandCreateRequest("Asus", null, null, null))))
                .andReturn();
        long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        mockMvc.perform(put("/admin/brands/" + id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new BrandUpdateRequest("Dell", null, "Mo ta moi", "https://example.com/dell.png"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Dell"))
                .andExpect(jsonPath("$.data.slug").value("dell"));
    }

    @Test
    void getOne_missing_returns404() throws Exception {
        String adminToken = registerAdminAndLogin("brand-admin-missing@example.com");

        mockMvc.perform(get("/admin/brands/999999").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void activateAndDeactivate_toggleStatus() throws Exception {
        String adminToken = registerAdminAndLogin("brand-admin-toggle@example.com");
        MvcResult createResult = mockMvc.perform(post("/admin/brands")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BrandCreateRequest("Lenovo", null, null, null))))
                .andReturn();
        long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data").path("id").asLong();

        mockMvc.perform(post("/admin/brands/" + id + "/deactivate").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        mockMvc.perform(post("/admin/brands/" + id + "/activate").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void list_returnsPagedBrands() throws Exception {
        String adminToken = registerAdminAndLogin("brand-admin-list@example.com");
        mockMvc.perform(post("/admin/brands")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new BrandCreateRequest("HP", null, null, null))));

        mockMvc.perform(get("/admin/brands?page=0&size=20").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    void asCustomer_returns403() throws Exception {
        String customerToken = registerCustomerAndLogin("brand-customer@example.com");

        mockMvc.perform(get("/admin/brands").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/admin/brands")).andExpect(status().isUnauthorized());
    }
}
