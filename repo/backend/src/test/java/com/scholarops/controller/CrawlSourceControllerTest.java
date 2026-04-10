package com.scholarops.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scholarops.model.dto.CrawlSourceRequest;
import com.scholarops.model.entity.CrawlSourceProfile;
import com.scholarops.security.JwtAuthenticationFilter;
import com.scholarops.security.JwtTokenProvider;
import com.scholarops.service.CrawlSourceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = CrawlSourceController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
class CrawlSourceControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private CrawlSourceService crawlSourceService;
    @MockBean private JwtTokenProvider jwtTokenProvider;

    @Test
    @WithMockUser(roles = "CONTENT_CURATOR")
    void testListSources() throws Exception {
        CrawlSourceProfile source = CrawlSourceProfile.builder()
                .id(1L).name("Test Source").baseUrl("https://example.com").build();

        when(crawlSourceService.listSources(any())).thenReturn(List.of(source));

        mockMvc.perform(get("/api/crawl-sources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "CONTENT_CURATOR")
    void testCreateSource() throws Exception {
        CrawlSourceRequest request = new CrawlSourceRequest();
        request.setName("New Source");
        request.setBaseUrl("https://newsource.com");

        CrawlSourceProfile created = CrawlSourceProfile.builder()
                .id(1L).name("New Source").baseUrl("https://newsource.com").build();

        when(crawlSourceService.createSource(any(), any())).thenReturn(created);

        mockMvc.perform(post("/api/crawl-sources")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("New Source"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/crawl-sources"))
                .andExpect(status().isForbidden());
    }
}
