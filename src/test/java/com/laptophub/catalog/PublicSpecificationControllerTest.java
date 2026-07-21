package com.laptophub.catalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Migration V12 đã seed sẵn ~9 spec baseline (category_id = NULL).
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PublicSpecificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void list_withoutAuth_returnsSeedBaseline() throws Exception {
        mockMvc.perform(get("/public/specifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code == 'cpu')]").exists());
    }
}
