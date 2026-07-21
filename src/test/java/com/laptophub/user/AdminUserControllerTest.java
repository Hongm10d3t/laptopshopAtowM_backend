package com.laptophub.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.auth.dto.LoginRequest;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String loginAndGetAccessToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private String registerAdminAndLogin(String email) throws Exception {
        userRepository.saveAndFlush(User.create(email, passwordEncoder.encode("Sup3rSecret!"), "Admin", null,
                UserRole.ADMIN));
        return loginAndGetAccessToken(email, "Sup3rSecret!");
    }

    private Long registerCustomer(String email) {
        User user = userRepository.saveAndFlush(User.create(email, passwordEncoder.encode("Sup3rSecret!"), "Cust",
                null, UserRole.CUSTOMER));
        return user.getId();
    }

    @Test
    void list_returnsUsers_filteredByKeyword() throws Exception {
        String adminToken = registerAdminAndLogin("admin-list@example.com");
        registerCustomer("keyword-match@example.com");

        mockMvc.perform(get("/admin/users?keyword=keyword-match")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].email").value("keyword-match@example.com"));
    }

    @Test
    void list_asCustomer_returns403() throws Exception {
        registerCustomer("plain-customer@example.com");
        String customerToken = loginAndGetAccessToken("plain-customer@example.com", "Sup3rSecret!");

        mockMvc.perform(get("/admin/users").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOne_returnsUserDetail() throws Exception {
        String adminToken = registerAdminAndLogin("admin-getone@example.com");
        Long targetId = registerCustomer("target-getone@example.com");

        mockMvc.perform(get("/admin/users/" + targetId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("target-getone@example.com"));
    }

    @Test
    void block_blocksTargetUser() throws Exception {
        String adminToken = registerAdminAndLogin("admin-block@example.com");
        Long targetId = registerCustomer("target-block@example.com");

        mockMvc.perform(post("/admin/users/" + targetId + "/block")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BLOCKED"));
    }

    @Test
    void block_self_returns400() throws Exception {
        String email = "admin-self-block@example.com";
        User admin = userRepository.saveAndFlush(
                User.create(email, passwordEncoder.encode("Sup3rSecret!"), "Admin", null, UserRole.ADMIN));
        String adminToken = loginAndGetAccessToken(email, "Sup3rSecret!");

        mockMvc.perform(post("/admin/users/" + admin.getId() + "/block")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void activate_reactivatesBlockedUser() throws Exception {
        String adminToken = registerAdminAndLogin("admin-activate@example.com");
        User target = userRepository.saveAndFlush(
                User.create("target-activate@example.com", passwordEncoder.encode("Sup3rSecret!"), "Cust", null,
                        UserRole.CUSTOMER));
        target.block();
        userRepository.saveAndFlush(target);

        mockMvc.perform(post("/admin/users/" + target.getId() + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    @Test
    void list_withoutAccessToken_returns401() throws Exception {
        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isUnauthorized());
    }
}
