package com.laptophub.cart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.auth.dto.LoginRequest;
import com.laptophub.cart.dto.CartItemAddRequest;
import com.laptophub.cart.dto.CartItemUpdateRequest;
import com.laptophub.catalog.entity.Brand;
import com.laptophub.catalog.entity.Category;
import com.laptophub.catalog.entity.Product;
import com.laptophub.catalog.entity.ProductVariant;
import com.laptophub.catalog.repository.BrandRepository;
import com.laptophub.catalog.repository.CategoryRepository;
import com.laptophub.catalog.repository.ProductRepository;
import com.laptophub.catalog.repository.ProductVariantRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerCartControllerTest {

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

    private String registerAndLogin(String email) throws Exception {
        userRepository.saveAndFlush(User.create(email, passwordEncoder.encode("Sup3rSecret!"), "Name", null,
                UserRole.CUSTOMER));
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "Sup3rSecret!"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private Long newVariantId(String slug, BigDecimal price) {
        Long categoryId = categoryRepository.saveAndFlush(Category.create("Cat " + slug, "cat-" + slug, null)).getId();
        Long brandId = brandRepository.saveAndFlush(Brand.create("Brand " + slug, "brand-" + slug, null, null)).getId();
        Long productId = productRepository
                .saveAndFlush(Product.create(categoryId, brandId, "Product " + slug, slug, null, null)).getId();
        return productVariantRepository
                .saveAndFlush(ProductVariant.create(productId, "SKU-" + slug, null, price, null, null, null, null))
                .getId();
    }

    @Test
    void view_returnsEmptyCart_whenNothingAddedYet() throws Exception {
        String accessToken = registerAndLogin("cart-empty@example.com");

        mockMvc.perform(get("/customer/cart").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0))
                .andExpect(jsonPath("$.data.totalAmount").value(0));
    }

    @Test
    void addItem_thenView_showsLivePriceAndLineTotal() throws Exception {
        String accessToken = registerAndLogin("cart-add@example.com");
        Long variantId = newVariantId("cart-add", new BigDecimal("500.00"));

        mockMvc.perform(post("/customer/cart/items")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CartItemAddRequest(variantId, 2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.quantity").value(2))
                .andExpect(jsonPath("$.data.lineTotal").value(1000.00));

        mockMvc.perform(get("/customer/cart").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.totalAmount").value(1000.00));
    }

    @Test
    void addItem_sameVariantTwice_accumulatesQuantity() throws Exception {
        String accessToken = registerAndLogin("cart-accumulate@example.com");
        Long variantId = newVariantId("cart-accumulate", BigDecimal.TEN);

        mockMvc.perform(post("/customer/cart/items")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemAddRequest(variantId, 2))));

        mockMvc.perform(post("/customer/cart/items")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CartItemAddRequest(variantId, 3))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.quantity").value(5));

        mockMvc.perform(get("/customer/cart").header("Authorization", "Bearer " + accessToken))
                .andExpect(jsonPath("$.data.items.length()").value(1));
    }

    @Test
    void addItem_unknownVariant_returns404() throws Exception {
        String accessToken = registerAndLogin("cart-unknown-variant@example.com");

        mockMvc.perform(post("/customer/cart/items")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CartItemAddRequest(999999L, 1))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void updateQuantity_changesQuantity() throws Exception {
        String accessToken = registerAndLogin("cart-update@example.com");
        Long variantId = newVariantId("cart-update", BigDecimal.TEN);
        MvcResult addResult = mockMvc.perform(post("/customer/cart/items")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CartItemAddRequest(variantId, 1))))
                .andReturn();
        long itemId = objectMapper.readTree(addResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(put("/customer/cart/items/" + itemId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CartItemUpdateRequest(7))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(7));
    }

    @Test
    void updateQuantity_ofAnotherUsersItem_returns404() throws Exception {
        String ownerToken = registerAndLogin("cart-update-owner@example.com");
        Long variantId = newVariantId("cart-update-owner", BigDecimal.TEN);
        MvcResult addResult = mockMvc.perform(post("/customer/cart/items")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CartItemAddRequest(variantId, 1))))
                .andReturn();
        long itemId = objectMapper.readTree(addResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        String strangerToken = registerAndLogin("cart-update-stranger@example.com");

        mockMvc.perform(put("/customer/cart/items/" + itemId)
                        .header("Authorization", "Bearer " + strangerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CartItemUpdateRequest(7))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void removeItem_deletesItemFromCart() throws Exception {
        String accessToken = registerAndLogin("cart-remove@example.com");
        Long variantId = newVariantId("cart-remove", BigDecimal.TEN);
        MvcResult addResult = mockMvc.perform(post("/customer/cart/items")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CartItemAddRequest(variantId, 1))))
                .andReturn();
        long itemId = objectMapper.readTree(addResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(delete("/customer/cart/items/" + itemId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/customer/cart").header("Authorization", "Bearer " + accessToken))
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    void clear_removesAllItems() throws Exception {
        String accessToken = registerAndLogin("cart-clear@example.com");
        Long variantId1 = newVariantId("cart-clear-1", BigDecimal.TEN);
        Long variantId2 = newVariantId("cart-clear-2", BigDecimal.TEN);
        mockMvc.perform(post("/customer/cart/items")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemAddRequest(variantId1, 1))));
        mockMvc.perform(post("/customer/cart/items")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemAddRequest(variantId2, 1))));

        mockMvc.perform(delete("/customer/cart").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/customer/cart").header("Authorization", "Bearer " + accessToken))
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    void view_withoutAccessToken_returns401() throws Exception {
        mockMvc.perform(get("/customer/cart"))
                .andExpect(status().isUnauthorized());
    }
}
