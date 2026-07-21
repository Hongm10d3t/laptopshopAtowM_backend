package com.laptophub.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.auth.dto.LoginRequest;
import com.laptophub.user.dto.AddressCreateRequest;
import com.laptophub.user.dto.AddressUpdateRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerAddressControllerTest {

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

    private String registerAndLogin(String email) throws Exception {
        userRepository.saveAndFlush(User.create(email, passwordEncoder.encode("Sup3rSecret!"), "Name", null,
                UserRole.CUSTOMER));
        return loginAndGetAccessToken(email, "Sup3rSecret!");
    }

    private AddressCreateRequest createRequest(Boolean isDefault) {
        return new AddressCreateRequest("Nguyen Van A", "0900000000", "HN", "CG", "DV", "123 Duong ABC", isDefault);
    }

    @Test
    void create_firstAddress_becomesDefault() throws Exception {
        String accessToken = registerAndLogin("addr-first@example.com");

        mockMvc.perform(post("/customer/addresses")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest(false))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.isDefault").value(true));
    }

    @Test
    void list_returnsOnlyOwnAddresses() throws Exception {
        String accessToken = registerAndLogin("addr-list@example.com");
        mockMvc.perform(post("/customer/addresses")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest(false))));

        mockMvc.perform(get("/customer/addresses").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void getOne_ofAnotherUser_returns404() throws Exception {
        String ownerToken = registerAndLogin("addr-owner@example.com");
        MvcResult createResult = mockMvc.perform(post("/customer/addresses")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest(false))))
                .andReturn();
        long addressId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        String strangerToken = registerAndLogin("addr-stranger@example.com");

        mockMvc.perform(get("/customer/addresses/" + addressId)
                        .header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void update_changesFields() throws Exception {
        String accessToken = registerAndLogin("addr-update@example.com");
        MvcResult createResult = mockMvc.perform(post("/customer/addresses")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest(false))))
                .andReturn();
        long addressId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(put("/customer/addresses/" + addressId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AddressUpdateRequest("New Name", "0922222222", "DN", "HC", "TK", "789"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipientName").value("New Name"));
    }

    @Test
    void delete_removesAddress() throws Exception {
        String accessToken = registerAndLogin("addr-delete@example.com");
        MvcResult createResult = mockMvc.perform(post("/customer/addresses")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest(false))))
                .andReturn();
        long addressId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(delete("/customer/addresses/" + addressId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/customer/addresses/" + addressId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void setDefault_switchesDefaultAddress() throws Exception {
        String accessToken = registerAndLogin("addr-default@example.com");
        mockMvc.perform(post("/customer/addresses")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest(false))));
        MvcResult secondResult = mockMvc.perform(post("/customer/addresses")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest(false))))
                .andReturn();
        long secondId = objectMapper.readTree(secondResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(post("/customer/addresses/" + secondId + "/default")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDefault").value(true));
    }

    @Test
    void create_withoutAccessToken_returns401() throws Exception {
        mockMvc.perform(post("/customer/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest(false))))
                .andExpect(status().isUnauthorized());
    }
}
