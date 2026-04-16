package com.scholarops.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scholarops.controller.support.AbstractWebMvcControllerTest;
import com.scholarops.model.dto.CrawlSourceRequest;
import com.scholarops.model.entity.CrawlSourceProfile;
import com.scholarops.security.JwtAuthenticationFilter;
import com.scholarops.security.JwtTokenProvider;
import com.scholarops.service.CrawlSourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.scholarops.controller.support.WebMvcTestUsers.userDetails;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = CrawlSourceController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
class CrawlSourceControllerTest extends AbstractWebMvcControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private CrawlSourceService crawlSourceService;
    @MockBean private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void grantPerms() {
        grantAllEvaluatorPermissions();
    }

    @Test
    void testListSources() throws Exception {
        CrawlSourceProfile source = CrawlSourceProfile.builder()
                .id(1L).name("Test Source").baseUrl("https://example.com").build();

        when(crawlSourceService.listSources(any())).thenReturn(List.of(source));

        mockMvc.perform(get("/api/crawl-sources")
                        .with(user(userDetails(8L, "curator", "CONTENT_CURATOR", "CRAWL_SOURCE_MANAGE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void testCreateSource() throws Exception {
        CrawlSourceRequest request = new CrawlSourceRequest();
        request.setName("New Source");
        request.setBaseUrl("https://newsource.com");

        CrawlSourceProfile created = CrawlSourceProfile.builder()
                .id(1L).name("New Source").baseUrl("https://newsource.com").build();

        when(crawlSourceService.createSource(any(), any())).thenReturn(created);

        mockMvc.perform(post("/api/crawl-sources")
                        .with(csrf())
                        .with(user(userDetails(8L, "curator", "CONTENT_CURATOR", "CRAWL_SOURCE_MANAGE")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("New Source"));
    }

    @Test
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/crawl-sources")
                        .with(user(userDetails(9L, "stu", "STUDENT", "QUIZ_TAKE"))))
                .andExpect(status().isForbidden());
    }
}
