package com.scholarops.controller;

import com.scholarops.controller.support.AbstractWebMvcControllerTest;
import com.scholarops.model.entity.StandardizedContentRecord;
import com.scholarops.security.JwtAuthenticationFilter;
import com.scholarops.security.JwtTokenProvider;
import com.scholarops.service.CatalogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = CatalogController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
class CatalogControllerTest extends AbstractWebMvcControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private CatalogService catalogService;
    @MockBean private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void grantPerms() {
        grantAllEvaluatorPermissions();
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void testSearchCatalog() throws Exception {
        StandardizedContentRecord record = new StandardizedContentRecord();
        record.setId(1L);
        record.setTitle("Test Content");
        record.setIsPublished(true);
        Page<StandardizedContentRecord> page = new PageImpl<>(List.of(record));

        when(catalogService.search(any())).thenReturn(page);

        mockMvc.perform(get("/api/catalog")
                        .param("keyword", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void testGetCatalogItem() throws Exception {
        StandardizedContentRecord record = new StandardizedContentRecord();
        record.setId(1L);
        record.setTitle("Test Item");

        when(catalogService.getPublishedContentById(1L)).thenReturn(record);

        mockMvc.perform(get("/api/catalog/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Test Item"));
    }
}
