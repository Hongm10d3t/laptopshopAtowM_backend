package com.laptophub.order;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.laptophub.auth.dto.LoginRequest;
import com.laptophub.cart.dto.CartItemAddRequest;
import com.laptophub.catalog.entity.Brand;
import com.laptophub.catalog.entity.Category;
import com.laptophub.catalog.entity.Product;
import com.laptophub.catalog.entity.ProductVariant;
import com.laptophub.catalog.repository.BrandRepository;
import com.laptophub.catalog.repository.CategoryRepository;
import com.laptophub.catalog.repository.ProductRepository;
import com.laptophub.catalog.repository.ProductVariantRepository;
import com.laptophub.inventory.service.InventoryService;
import com.laptophub.order.dto.CheckoutRequest;
import com.laptophub.user.dto.AddressCreateRequest;
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
class CustomerOrderControllerTest {

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
    private InventoryService inventoryService;

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

    private Long newVariantId(String slug, BigDecimal price, int stock) {
        Long categoryId = categoryRepository.saveAndFlush(Category.create("Cat " + slug, "cat-" + slug, null)).getId();
        Long brandId = brandRepository.saveAndFlush(Brand.create("Brand " + slug, "brand-" + slug, null, null)).getId();
        Long productId = productRepository
                .saveAndFlush(Product.create(categoryId, brandId, "Product " + slug, slug, null, null)).getId();
        Long variantId = productVariantRepository
                .saveAndFlush(ProductVariant.create(productId, "SKU-" + slug, "16GB/512GB", price, 16, 512, "SSD",
                        "Black"))
                .getId();
        if (stock > 0) {
            inventoryService.receiveStock(variantId, stock, "TEST_SEED", null, null);
        }
        return variantId;
    }

    private long createAddress(String accessToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/customer/addresses")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddressCreateRequest("Nguyen Van A",
                                "0900000000", "HN", "CG", "DV", "123 Duong ABC", true))))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("id").asLong();
    }

    private void addToCart(String accessToken, Long variantId, int quantity) throws Exception {
        mockMvc.perform(post("/customer/cart/items")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemAddRequest(variantId, quantity))));
    }

    @Test
    void checkout_success_createsOrderWithSnapshotAndReservesStock() throws Exception {
        String accessToken = registerAndLogin("order-checkout@example.com");
        Long variantId = newVariantId("order-checkout", new BigDecimal("500.00"), 10);
        long addressId = createAddress(accessToken);
        addToCart(accessToken, variantId, 2);

        mockMvc.perform(post("/customer/orders")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckoutRequest(addressId, "Giao giờ hành chính"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.paymentMethod").value("COD"))
                .andExpect(jsonPath("$.data.totalAmount").value(1000.00))
                .andExpect(jsonPath("$.data.recipientName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.items[0].sku").value("SKU-order-checkout"));

        mockMvc.perform(get("/admin/inventory/" + variantId + "/balance")
                        .header("Authorization", "Bearer " + registerAdminAndLogin("order-checkout-admin@example.com")))
                .andExpect(jsonPath("$.data.reservedQuantity").value(2));
    }

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

    @Test
    void checkout_emptyCart_returns400() throws Exception {
        String accessToken = registerAndLogin("order-empty-cart@example.com");
        long addressId = createAddress(accessToken);

        mockMvc.perform(post("/customer/orders")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckoutRequest(addressId, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void checkout_addressNotOwned_returns404() throws Exception {
        String ownerToken = registerAndLogin("order-addr-owner@example.com");
        long ownerAddressId = createAddress(ownerToken);
        String strangerToken = registerAndLogin("order-addr-stranger@example.com");
        Long variantId = newVariantId("order-addr-stranger", BigDecimal.TEN, 10);
        addToCart(strangerToken, variantId, 1);

        mockMvc.perform(post("/customer/orders")
                        .header("Authorization", "Bearer " + strangerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckoutRequest(ownerAddressId, null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void checkout_insufficientStock_returns409() throws Exception {
        String accessToken = registerAndLogin("order-insufficient-stock@example.com");
        Long variantId = newVariantId("order-insufficient-stock", BigDecimal.TEN, 1);
        long addressId = createAddress(accessToken);
        addToCart(accessToken, variantId, 5);

        mockMvc.perform(post("/customer/orders")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckoutRequest(addressId, null))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"));
    }

    @Test
    void list_returnsOnlyOwnOrders() throws Exception {
        String accessToken = registerAndLogin("order-list@example.com");
        Long variantId = newVariantId("order-list", BigDecimal.TEN, 10);
        long addressId = createAddress(accessToken);
        addToCart(accessToken, variantId, 1);
        mockMvc.perform(post("/customer/orders")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CheckoutRequest(addressId, null))));

        mockMvc.perform(get("/customer/orders").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));
    }

    @Test
    void getOne_ofAnotherUser_returns404() throws Exception {
        String ownerToken = registerAndLogin("order-detail-owner@example.com");
        Long variantId = newVariantId("order-detail-owner", BigDecimal.TEN, 10);
        long addressId = createAddress(ownerToken);
        addToCart(ownerToken, variantId, 1);
        MvcResult checkoutResult = mockMvc.perform(post("/customer/orders")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckoutRequest(addressId, null))))
                .andReturn();
        long orderId = objectMapper.readTree(checkoutResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        String strangerToken = registerAndLogin("order-detail-stranger@example.com");

        mockMvc.perform(get("/customer/orders/" + orderId).header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void checkout_withoutAccessToken_returns401() throws Exception {
        mockMvc.perform(post("/customer/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckoutRequest(1L, null))))
                .andExpect(status().isUnauthorized());
    }
}
