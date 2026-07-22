package com.laptophub.inventory.repository;

import com.laptophub.catalog.entity.Brand;
import com.laptophub.catalog.entity.Category;
import com.laptophub.catalog.entity.Product;
import com.laptophub.catalog.entity.ProductVariant;
import com.laptophub.catalog.repository.BrandRepository;
import com.laptophub.catalog.repository.CategoryRepository;
import com.laptophub.catalog.repository.ProductRepository;
import com.laptophub.catalog.repository.ProductVariantRepository;
import com.laptophub.config.JpaAuditingConfig;
import com.laptophub.inventory.entity.StockReceipt;
import com.laptophub.inventory.entity.StockReceiptItem;
import com.laptophub.user.EmailNormalizer;
import com.laptophub.user.entity.User;
import com.laptophub.user.entity.UserRole;
import com.laptophub.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class StockReceiptItemRepositoryTest {

    @Autowired
    private StockReceiptItemRepository stockReceiptItemRepository;

    @Autowired
    private StockReceiptRepository stockReceiptRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private UserRepository userRepository;

    private Long newVariantId(String slug) {
        Long categoryId = categoryRepository.saveAndFlush(Category.create("Cat " + slug, "cat-" + slug, null)).getId();
        Long brandId = brandRepository.saveAndFlush(Brand.create("Brand " + slug, "brand-" + slug, null, null)).getId();
        Long productId = productRepository
                .saveAndFlush(Product.create(categoryId, brandId, "Product " + slug, slug, null, null)).getId();
        return productVariantRepository
                .saveAndFlush(ProductVariant.create(productId, "SKU-" + slug, null, BigDecimal.TEN, null, null,
                        null, null))
                .getId();
    }

    private Long newReceiptId(String code) {
        Long userId = userRepository
                .saveAndFlush(User.create(EmailNormalizer.normalize(code + "@example.com"), "hash", "Admin", null,
                        UserRole.ADMIN))
                .getId();
        return stockReceiptRepository.saveAndFlush(StockReceipt.create(code, null, userId)).getId();
    }

    @Test
    void findByStockReceiptId_returnsAllItemsOfReceipt() {
        Long receiptId = newReceiptId("PN-ITEMS-FIND");
        Long variant1 = newVariantId("items-find-1");
        Long variant2 = newVariantId("items-find-2");
        stockReceiptItemRepository.saveAndFlush(StockReceiptItem.create(receiptId, variant1, 5));
        stockReceiptItemRepository.saveAndFlush(StockReceiptItem.create(receiptId, variant2, 3));

        List<StockReceiptItem> items = stockReceiptItemRepository.findByStockReceiptId(receiptId);

        assertThat(items).hasSize(2);
    }

    @Test
    void deleteByStockReceiptId_removesAllItemsOfReceipt() {
        Long receiptId = newReceiptId("PN-ITEMS-DELETE");
        Long variant1 = newVariantId("items-delete-1");
        stockReceiptItemRepository.saveAndFlush(StockReceiptItem.create(receiptId, variant1, 5));

        stockReceiptItemRepository.deleteByStockReceiptId(receiptId);

        assertThat(stockReceiptItemRepository.findByStockReceiptId(receiptId)).isEmpty();
    }
}
