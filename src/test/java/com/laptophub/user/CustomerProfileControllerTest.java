package com.laptophub.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.auth.dto.LoginRequest;
import com.laptophub.user.dto.UpdateProfileRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerProfileControllerTest {

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

    @Test
    void getProfile_returnsOwnProfile() throws Exception {
        userRepository.saveAndFlush(User.create("profile-get@example.com",
                passwordEncoder.encode("Sup3rSecret!"), "Nguyen Van A", "0900000000", UserRole.CUSTOMER));
        String accessToken = loginAndGetAccessToken("profile-get@example.com", "Sup3rSecret!");

        mockMvc.perform(get("/customer/profile").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("profile-get@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.data.phone").value("0900000000"));
    }

    @Test
    void getProfile_withoutAccessToken_returns401() throws Exception {
        mockMvc.perform(get("/customer/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    @Test
    void updateProfile_changesFullNameAndPhone() throws Exception {
        userRepository.saveAndFlush(User.create("profile-update@example.com",
                passwordEncoder.encode("Sup3rSecret!"), "Old Name", null, UserRole.CUSTOMER));
        String accessToken = loginAndGetAccessToken("profile-update@example.com", "Sup3rSecret!");

        mockMvc.perform(put("/customer/profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProfileRequest("New Name", "0911111111"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("New Name"))
                .andExpect(jsonPath("$.data.phone").value("0911111111"));

        User updated = userRepository.findByEmail("profile-update@example.com").orElseThrow();
        assertThat(updated.getFullName()).isEqualTo("New Name");
    }

    @Test
    void updateProfile_withBlankFullName_returns400() throws Exception {
        userRepository.saveAndFlush(User.create("profile-invalid@example.com",
                passwordEncoder.encode("Sup3rSecret!"), "Old Name", null, UserRole.CUSTOMER));
        String accessToken = loginAndGetAccessToken("profile-invalid@example.com", "Sup3rSecret!");

        mockMvc.perform(put("/customer/profile")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProfileRequest("", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void adminAccessingCustomerProfile_returns403() throws Exception {
        userRepository.saveAndFlush(User.create("admin-profile@example.com",
                passwordEncoder.encode("Sup3rSecret!"), "Admin", null, UserRole.ADMIN));
        String accessToken = loginAndGetAccessToken("admin-profile@example.com", "Sup3rSecret!");

        mockMvc.perform(get("/customer/profile").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }
}
