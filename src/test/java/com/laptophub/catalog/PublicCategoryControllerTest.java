package com.laptophub.catalog;

import com.laptophub.catalog.entity.Category;
import com.laptophub.catalog.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicCategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void list_returnsOnlyActiveCategories_noAuthRequired() throws Exception {
        Category active = Category.create("Laptop Public Active", "laptop-public-active", null);
        Category inactive = Category.create("Laptop Public Inactive", "laptop-public-inactive", null);
        inactive.deactivate();
        categoryRepository.saveAndFlush(active);
        categoryRepository.saveAndFlush(inactive);

        mockMvc.perform(get("/public/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.slug == 'laptop-public-active')]").exists())
                .andExpect(jsonPath("$.data[?(@.slug == 'laptop-public-inactive')]").doesNotExist());
    }
}
