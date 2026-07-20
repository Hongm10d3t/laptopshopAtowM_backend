package com.laptophub.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.auth.dto.RegisterRequest;
import com.laptophub.user.User;
import com.laptophub.user.UserRepository;
import com.laptophub.user.UserRole;
import com.laptophub.user.UserStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Test HTTP thật đầu tiên của project (qua endpoint /auth/register thật, không
// còn phải suy luận gián tiếp qua SecurityProbeController như các gói trước).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerRegisterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void register_validRequest_returns201_persistsCorrectUser_andDoesNotLeakPassword() throws Exception {
        // Không thêm khoảng trắng đầu/cuối vào email: @Email validate chuỗi
        // thô TRƯỚC khi EmailNormalizer.normalize() (trim) chạy trong
        // RegisterService, nên whitespace ở biên sẽ bị 400 thay vì được trim
        // — giới hạn thiết kế đã biết, không phải phạm vi sửa của gói này.
        // Vẫn giữ chữ hoa/thường trộn để xác nhận phần lowercase hoạt động
        // đúng qua HTTP thật.
        RegisterRequest request = new RegisterRequest(
                "NewUser@Example.COM", "Sup3rSecret!", "Nguyen Van A", "0901234567");

        String responseBody = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Nguyen Van A"))
                .andReturn().getResponse().getContentAsString();

        // Không lộ password/hash ở bất kỳ đâu trong response — kiểm tra cả
        // response thô, không chỉ field đã biết trước.
        assertThat(responseBody.toLowerCase()).doesNotContain("password").doesNotContain("hash");

        // Không phát token: register chỉ tạo account.
        assertThat(responseBody).doesNotContain("accessToken").doesNotContain("refreshToken");

        // User trong DB đúng: role CUSTOMER, status ACTIVE, password đã hash thật.
        Optional<User> saved = userRepository.findByEmail("newuser@example.com");
        assertThat(saved).isPresent();
        assertThat(saved.get().getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(saved.get().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.get().getPasswordHash()).isNotEqualTo("Sup3rSecret!");
        assertThat(passwordEncoder.matches("Sup3rSecret!", saved.get().getPasswordHash())).isTrue();
    }

    @Test
    void register_blankEmail_returns400ValidationError() throws Exception {
        RegisterRequest request = new RegisterRequest("", "Sup3rSecret!", "Nguyen Van A", null);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        RegisterRequest first = new RegisterRequest("dup-http@example.com", "Sup3rSecret!", "Name", null);
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        RegisterRequest duplicate = new RegisterRequest("dup-http@example.com", "AnotherPass1!", "Other Name", null);
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_EXISTS"));
    }
}
