package com.laptophub.catalog;

import com.laptophub.catalog.entity.Brand;
import com.laptophub.catalog.repository.BrandRepository;
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
class PublicBrandControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BrandRepository brandRepository;

    @Test
    void list_returnsOnlyActiveBrands_noAuthRequired() throws Exception {
        Brand active = Brand.create("Asus Public Active", "asus-public-active", null, null);
        Brand inactive = Brand.create("Asus Public Inactive", "asus-public-inactive", null, null);
        inactive.deactivate();
        brandRepository.saveAndFlush(active);
        brandRepository.saveAndFlush(inactive);

        mockMvc.perform(get("/public/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.slug == 'asus-public-active')]").exists())
                .andExpect(jsonPath("$.data[?(@.slug == 'asus-public-inactive')]").doesNotExist());
    }
}
