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

// AdminOrderController thao tác trực tiếp trên DB thật (không mock) — bắt
// buộc để chứng minh OrderService.ship tuân đúng thứ tự "load lại Order sau
// khi gọi InventoryService.fulfill" (@Modifying(clearAutomatically = true)
// xoá persistence context). Test bằng Mockito sẽ luôn pass kể cả nếu quên
// reload, vì mock trả về đúng object Java tham chiếu bất kể có "clear" hay
// không — chỉ DB thật mới lộ ra bug này.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminOrderControllerTest {

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

    private String login(String email, UserRole role) throws Exception {
        userRepository.saveAndFlush(User.create(email, passwordEncoder.encode("Sup3rSecret!"),
                role == UserRole.ADMIN ? "Admin" : "Customer", null, role));
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "Sup3rSecret!"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    private Long newVariantId(String slug, int stock) {
        Long categoryId = categoryRepository.saveAndFlush(Category.create("Cat " + slug, "cat-" + slug, null)).getId();
        Long brandId = brandRepository.saveAndFlush(Brand.create("Brand " + slug, "brand-" + slug, null, null)).getId();
        Long productId = productRepository
                .saveAndFlush(Product.create(categoryId, brandId, "Product " + slug, slug, null, null)).getId();
        Long variantId = productVariantRepository
                .saveAndFlush(ProductVariant.create(productId, "SKU-" + slug, null, new BigDecimal("100.00"), null,
                        null, null, null))
                .getId();
        inventoryService.receiveStock(variantId, stock, "TEST_SEED", null, null);
        return variantId;
    }

    // Đăng ký + đăng nhập Customer, thêm 1 sản phẩm vào giỏ, checkout — trả
    // về id đơn vừa tạo (PENDING).
    private long createPendingOrder(String slug, int quantity, int stock) throws Exception {
        String customerToken = login(slug + "-customer@example.com", UserRole.CUSTOMER);
        Long variantId = newVariantId(slug, stock);

        MvcResult addressResult = mockMvc.perform(post("/customer/addresses")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AddressCreateRequest("Nguyen Van A",
                                "0900000000", "HN", "CG", "DV", "123 Duong ABC", true))))
                .andReturn();
        long addressId = objectMapper.readTree(addressResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(post("/customer/cart/items")
                .header("Authorization", "Bearer " + customerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new CartItemAddRequest(variantId, quantity))));

        MvcResult checkoutResult = mockMvc.perform(post("/customer/orders")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckoutRequest(addressId, null))))
                .andReturn();
        return objectMapper.readTree(checkoutResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    @Test
    void confirm_transitionsPendingToConfirmed() throws Exception {
        long orderId = createPendingOrder("admin-confirm", 1, 10);
        String adminToken = login("admin-confirm-admin@example.com", UserRole.ADMIN);

        mockMvc.perform(post("/admin/orders/" + orderId + "/confirm")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    void confirm_rejectsSecondCall_returns400() throws Exception {
        long orderId = createPendingOrder("admin-confirm-twice", 1, 10);
        String adminToken = login("admin-confirm-twice-admin@example.com", UserRole.ADMIN);
        mockMvc.perform(post("/admin/orders/" + orderId + "/confirm")
                .header("Authorization", "Bearer " + adminToken));

        mockMvc.perform(post("/admin/orders/" + orderId + "/confirm")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ORDER_STATUS"));
    }

    @Test
    void ship_decreasesOnHandAndReserved_andPersistsShippingStatus() throws Exception {
        long orderId = createPendingOrder("admin-ship", 3, 10);
        String adminToken = login("admin-ship-admin@example.com", UserRole.ADMIN);
        mockMvc.perform(post("/admin/orders/" + orderId + "/confirm").header("Authorization", "Bearer " + adminToken));
        mockMvc.perform(post("/admin/orders/" + orderId + "/prepare").header("Authorization", "Bearer " + adminToken));

        mockMvc.perform(post("/admin/orders/" + orderId + "/ship")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SHIPPING"));

        // Đọc lại qua GET riêng (request khác, không dùng lại object Java vừa
        // trả về ở trên) để chắc chắn status SHIPPING đã thật sự được flush
        // xuống DB, không chỉ nằm trên object Java bị mất do persistence
        // context bị clear giữa chừng (@Modifying(clearAutomatically = true)
        // của InventoryBalanceRepository.fulfillQuantity).
        MvcResult getResult = mockMvc.perform(get("/admin/orders/" + orderId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.data.status").value("SHIPPING"))
                .andReturn();
        long variantId = objectMapper.readTree(getResult.getResponse().getContentAsString())
                .path("data").path("items").get(0).path("productVariantId").asLong();

        mockMvc.perform(get("/admin/inventory/" + variantId + "/balance")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(jsonPath("$.data.onHandQuantity").value(7))
                .andExpect(jsonPath("$.data.reservedQuantity").value(0));
    }

    @Test
    void ship_rejectsWhenNotPreparing_returns400() throws Exception {
        long orderId = createPendingOrder("admin-ship-invalid", 1, 10);
        String adminToken = login("admin-ship-invalid-admin@example.com", UserRole.ADMIN);

        mockMvc.perform(post("/admin/orders/" + orderId + "/ship")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_ORDER_STATUS"));
    }

    @Test
    void deliver_transitionsShippingToDelivered() throws Exception {
        long orderId = createPendingOrder("admin-deliver", 1, 10);
        String adminToken = login("admin-deliver-admin@example.com", UserRole.ADMIN);
        mockMvc.perform(post("/admin/orders/" + orderId + "/confirm").header("Authorization", "Bearer " + adminToken));
        mockMvc.perform(post("/admin/orders/" + orderId + "/prepare").header("Authorization", "Bearer " + adminToken));
        mockMvc.perform(post("/admin/orders/" + orderId + "/ship").header("Authorization", "Bearer " + adminToken));

        mockMvc.perform(post("/admin/orders/" + orderId + "/deliver")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DELIVERED"));
    }

    @Test
    void list_filtersByStatus() throws Exception {
        long orderId = createPendingOrder("admin-list", 1, 10);
        String adminToken = login("admin-list-admin@example.com", UserRole.ADMIN);

        mockMvc.perform(get("/admin/orders").param("status", "PENDING")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == " + orderId + ")]").exists());
    }

    @Test
    void getOne_ofAnyCustomer_returns200() throws Exception {
        long orderId = createPendingOrder("admin-getone", 1, 10);
        String adminToken = login("admin-getone-admin@example.com", UserRole.ADMIN);

        mockMvc.perform(get("/admin/orders/" + orderId).header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(orderId));
    }

    @Test
    void adminEndpoints_rejectCustomerToken_returns403() throws Exception {
        long orderId = createPendingOrder("admin-forbidden", 1, 10);
        String customerToken = login("admin-forbidden-customer2@example.com", UserRole.CUSTOMER);

        mockMvc.perform(post("/admin/orders/" + orderId + "/confirm")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());
    }
}
