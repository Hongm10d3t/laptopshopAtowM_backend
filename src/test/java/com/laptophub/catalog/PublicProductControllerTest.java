package com.laptophub.catalog;

import com.laptophub.catalog.entity.Brand;
import com.laptophub.catalog.entity.Category;
import com.laptophub.catalog.entity.Product;
import com.laptophub.catalog.entity.ProductImage;
import com.laptophub.catalog.entity.ProductVariant;
import com.laptophub.catalog.repository.BrandRepository;
import com.laptophub.catalog.repository.CategoryRepository;
import com.laptophub.catalog.repository.ProductImageRepository;
import com.laptophub.catalog.repository.ProductRepository;
import com.laptophub.catalog.repository.ProductVariantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Test tích hợp end-to-end cho GET /public/products: dựng dữ liệu thật qua
// repository (category/brand/product/variant/ảnh), gọi qua MockMvc, không
// mock tầng nào — xác nhận toàn bộ chuỗi filter/sort/phân trang hoạt động
// đúng trên MySQL thật.
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    private Long newCategoryId(String slug) {
        return categoryRepository.saveAndFlush(Category.create("Cat " + slug, slug, null)).getId();
    }

    private Long newBrandId(String slug) {
        return brandRepository.saveAndFlush(Brand.create("Brand " + slug, slug, null, null)).getId();
    }

    private Product newProduct(Long categoryId, Long brandId, String name, String slug) {
        return productRepository.saveAndFlush(Product.create(categoryId, brandId, name, slug, "Ngan", "Dai"));
    }

    private void newVariant(Long productId, String sku, String price) {
        productVariantRepository.saveAndFlush(
                ProductVariant.create(productId, sku, null, new BigDecimal(price), null, null, null, null));
    }

    @Test
    void search_returnsOnlyProductWithActiveVariantAndActiveCategoryBrand() throws Exception {
        Long categoryId = newCategoryId("cat-public-search-visible");
        Long brandId = newBrandId("brand-public-search-visible");
        Product visible = newProduct(categoryId, brandId, "Laptop Public Visible", "laptop-public-visible");
        newVariant(visible.getId(), "SKU-PUB-VIS-1", "999.00");

        newProduct(categoryId, brandId, "Laptop Public No Variant", "laptop-public-novariant");

        Product inactiveProduct = newProduct(categoryId, brandId, "Laptop Public Inactive", "laptop-public-inactive");
        inactiveProduct.deactivate();
        productRepository.saveAndFlush(inactiveProduct);
        newVariant(inactiveProduct.getId(), "SKU-PUB-INACTIVE-1", "500.00");

        mockMvc.perform(get("/public/products").param("keyword", "laptop public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.slug == 'laptop-public-visible')]").exists())
                .andExpect(jsonPath("$.data.content[?(@.slug == 'laptop-public-novariant')]").doesNotExist())
                .andExpect(jsonPath("$.data.content[?(@.slug == 'laptop-public-inactive')]").doesNotExist());
    }

    @Test
    void search_hidesProduct_whenCategoryDeactivated() throws Exception {
        Long categoryId = newCategoryId("cat-public-search-hiddencat");
        Long brandId = newBrandId("brand-public-search-hiddencat");
        Product product = newProduct(categoryId, brandId, "Laptop Hidden Category", "laptop-public-hiddencat");
        newVariant(product.getId(), "SKU-PUB-HIDDENCAT-1", "999.00");

        Category category = categoryRepository.findById(categoryId).orElseThrow();
        category.deactivate();
        categoryRepository.saveAndFlush(category);

        mockMvc.perform(get("/public/products").param("keyword", "hidden category"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    void search_filtersByPriceRange() throws Exception {
        Long categoryId = newCategoryId("cat-public-search-price");
        Long brandId = newBrandId("brand-public-search-price");
        Product cheap = newProduct(categoryId, brandId, "Laptop Cheap Price", "laptop-public-cheap-price");
        newVariant(cheap.getId(), "SKU-PUB-CHEAP-1", "300.00");
        Product expensive = newProduct(categoryId, brandId, "Laptop Expensive Price", "laptop-public-expensive-price");
        newVariant(expensive.getId(), "SKU-PUB-EXPENSIVE-1", "2000.00");

        mockMvc.perform(get("/public/products")
                        .param("keyword", "price")
                        .param("minPrice", "1000")
                        .param("maxPrice", "3000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.slug == 'laptop-public-expensive-price')]").exists())
                .andExpect(jsonPath("$.data.content[?(@.slug == 'laptop-public-cheap-price')]").doesNotExist());
    }

    @Test
    void search_sortsByPriceAsc() throws Exception {
        Long categoryId = newCategoryId("cat-public-search-sort");
        Long brandId = newBrandId("brand-public-search-sort");
        Product expensive = newProduct(categoryId, brandId, "Laptop Sort Expensive", "laptop-public-sort-expensive");
        newVariant(expensive.getId(), "SKU-PUB-SORT-EXP-1", "2000.00");
        Product cheap = newProduct(categoryId, brandId, "Laptop Sort Cheap", "laptop-public-sort-cheap");
        newVariant(cheap.getId(), "SKU-PUB-SORT-CHEAP-1", "300.00");

        mockMvc.perform(get("/public/products")
                        .param("keyword", "sort")
                        .param("sort", "PRICE_ASC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].slug").value("laptop-public-sort-cheap"))
                .andExpect(jsonPath("$.data.content[1].slug").value("laptop-public-sort-expensive"));
    }

    @Test
    void search_paginatesResults() throws Exception {
        Long categoryId = newCategoryId("cat-public-search-page");
        Long brandId = newBrandId("brand-public-search-page");
        for (int i = 0; i < 3; i++) {
            Product product = newProduct(categoryId, brandId, "Laptop Public Page " + i, "laptop-public-page-" + i);
            newVariant(product.getId(), "SKU-PUB-PAGE-" + i, "100.00");
        }

        mockMvc.perform(get("/public/products")
                        .param("keyword", "laptop public page")
                        .param("sort", "NAME_ASC")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2));
    }

    @Test
    void getDetail_returnsFullDetailWithActiveVariantsAndImages() throws Exception {
        Long categoryId = newCategoryId("cat-public-detail");
        Long brandId = newBrandId("brand-public-detail");
        Product product = newProduct(categoryId, brandId, "Laptop Detail", "laptop-public-detail");
        newVariant(product.getId(), "SKU-PUB-DETAIL-1", "999.00");
        productImageRepository.saveAndFlush(
                ProductImage.create(product.getId(), "https://example.com/detail.png", "Anh", 0));

        mockMvc.perform(get("/public/products/laptop-public-detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Laptop Detail"))
                .andExpect(jsonPath("$.data.categoryName").value("Cat cat-public-detail"))
                .andExpect(jsonPath("$.data.brandName").value("Brand brand-public-detail"))
                .andExpect(jsonPath("$.data.variants.length()").value(1))
                .andExpect(jsonPath("$.data.images.length()").value(1));
    }

    @Test
    void getDetail_returns404_whenProductInactive() throws Exception {
        Long categoryId = newCategoryId("cat-public-detail-inactive");
        Long brandId = newBrandId("brand-public-detail-inactive");
        Product product = newProduct(categoryId, brandId, "Laptop Detail Inactive", "laptop-public-detail-inactive");
        product.deactivate();
        productRepository.saveAndFlush(product);

        mockMvc.perform(get("/public/products/laptop-public-detail-inactive"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void getDetail_returns404_whenSlugMissing() throws Exception {
        mockMvc.perform(get("/public/products/khong-ton-tai"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }
}
