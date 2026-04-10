package com.scholarops.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scholarops.exception.ResourceNotFoundException;
import com.scholarops.model.entity.MediaMetadata;
import com.scholarops.model.entity.StandardizedContentRecord;
import com.scholarops.security.JwtAuthenticationFilter;
import com.scholarops.security.JwtTokenProvider;
import com.scholarops.service.CatalogService;
import com.scholarops.service.ContentStandardizationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests catalog access control for published vs unpublished content and
 * permission-based access across different roles.
 *
 * The CatalogController only requires hasPermission(null, 'CONTENT_VIEW'),
 * with no specific role restriction. The ContentController requires
 * CONTENT_CURATOR role + CONTENT_REVIEW permission.
 */
@WebMvcTest(
        value = {CatalogController.class, ContentController.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class))
class CatalogAccessTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private CatalogService catalogService;
    @MockBean private ContentStandardizationService contentStandardizationService;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private PermissionEvaluator permissionEvaluator;

    // -----------------------------------------------------------------------
    // Catalog access with CONTENT_VIEW permission (any authenticated role)
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Catalog accessible to any role with CONTENT_VIEW permission")
    class CatalogWithContentView {

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student with CONTENT_VIEW can search catalog")
        void studentCanSearchCatalog() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("CONTENT_VIEW")))
                    .thenReturn(true);

            StandardizedContentRecord record = new StandardizedContentRecord();
            record.setId(1L);
            record.setTitle("Published Course");
            record.setIsPublished(true);
            Page<StandardizedContentRecord> page = new PageImpl<>(List.of(record));

            when(catalogService.search(any())).thenReturn(page);

            mockMvc.perform(get("/api/catalog").param("keyword", "course"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].title").value("Published Course"));
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student with CONTENT_VIEW can get catalog item by ID")
        void studentCanGetCatalogItem() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("CONTENT_VIEW")))
                    .thenReturn(true);

            StandardizedContentRecord record = new StandardizedContentRecord();
            record.setId(1L);
            record.setTitle("Test Item");
            record.setIsPublished(true);

            when(catalogService.getPublishedContentById(1L)).thenReturn(record);

            mockMvc.perform(get("/api/catalog/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.title").value("Test Item"));
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student receives 404 when requesting unpublished catalog item by ID")
        void studentGetsNotFoundForUnpublishedCatalogItem() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("CONTENT_VIEW")))
                    .thenReturn(true);

            when(catalogService.getPublishedContentById(99L))
                    .thenThrow(new ResourceNotFoundException("Content not found: 99"));

            mockMvc.perform(get("/api/catalog/99"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Instructor with CONTENT_VIEW can search catalog")
        void instructorCanSearchCatalog() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("CONTENT_VIEW")))
                    .thenReturn(true);

            Page<StandardizedContentRecord> page = new PageImpl<>(List.of());
            when(catalogService.search(any())).thenReturn(page);

            mockMvc.perform(get("/api/catalog"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Admin with CONTENT_VIEW can search catalog")
        void adminCanSearchCatalog() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("CONTENT_VIEW")))
                    .thenReturn(true);

            Page<StandardizedContentRecord> page = new PageImpl<>(List.of());
            when(catalogService.search(any())).thenReturn(page);

            mockMvc.perform(get("/api/catalog"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "CONTENT_CURATOR")
        @DisplayName("Curator with CONTENT_VIEW can search catalog")
        void curatorCanSearchCatalog() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("CONTENT_VIEW")))
                    .thenReturn(true);

            Page<StandardizedContentRecord> page = new PageImpl<>(List.of());
            when(catalogService.search(any())).thenReturn(page);

            mockMvc.perform(get("/api/catalog"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "TEACHING_ASSISTANT")
        @DisplayName("TA with CONTENT_VIEW can search catalog")
        void taCanSearchCatalog() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("CONTENT_VIEW")))
                    .thenReturn(true);

            Page<StandardizedContentRecord> page = new PageImpl<>(List.of());
            when(catalogService.search(any())).thenReturn(page);

            mockMvc.perform(get("/api/catalog"))
                    .andExpect(status().isOk());
        }
    }

    // -----------------------------------------------------------------------
    // Catalog access denied without CONTENT_VIEW permission
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Catalog inaccessible without CONTENT_VIEW permission")
    class CatalogWithoutContentView {

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student without CONTENT_VIEW cannot search catalog")
        void studentCannotSearchWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("CONTENT_VIEW")))
                    .thenReturn(false);

            mockMvc.perform(get("/api/catalog"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student without CONTENT_VIEW cannot get catalog item")
        void studentCannotGetItemWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("CONTENT_VIEW")))
                    .thenReturn(false);

            mockMvc.perform(get("/api/catalog/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Instructor without CONTENT_VIEW cannot search catalog")
        void instructorCannotSearchWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("CONTENT_VIEW")))
                    .thenReturn(false);

            mockMvc.perform(get("/api/catalog"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Admin without CONTENT_VIEW cannot search catalog")
        void adminCannotSearchWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("CONTENT_VIEW")))
                    .thenReturn(false);

            mockMvc.perform(get("/api/catalog"))
                    .andExpect(status().isForbidden());
        }
    }

    // -----------------------------------------------------------------------
    // Catalog is unauthenticated-denied
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Catalog requires authentication")
    class CatalogRequiresAuth {

        @Test
        @DisplayName("Unauthenticated user cannot search catalog")
        void unauthenticatedCannotSearchCatalog() throws Exception {
            mockMvc.perform(get("/api/catalog"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Unauthenticated user cannot get catalog item")
        void unauthenticatedCannotGetItem() throws Exception {
            mockMvc.perform(get("/api/catalog/1"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // -----------------------------------------------------------------------
    // Content management restricted to CONTENT_CURATOR role
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Content management restricted to CONTENT_CURATOR")
    class ContentManagementAccess {

        @Test
        @WithMockUser(roles = "CONTENT_CURATOR")
        @DisplayName("Curator with CONTENT_REVIEW can list content")
        void curatorCanListContent() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("CONTENT_REVIEW")))
                    .thenReturn(true);

            Page<StandardizedContentRecord> page = new PageImpl<>(List.of());
            when(contentStandardizationService.listContent(any())).thenReturn(page);

            mockMvc.perform(get("/api/content"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "CONTENT_CURATOR")
        @DisplayName("Curator without CONTENT_REVIEW cannot list content")
        void curatorCannotListWithoutPermission() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("CONTENT_REVIEW")))
                    .thenReturn(false);

            mockMvc.perform(get("/api/content"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student cannot access content management even with permission")
        void studentCannotAccessContentManagement() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), any()))
                    .thenReturn(true);

            mockMvc.perform(get("/api/content"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "INSTRUCTOR")
        @DisplayName("Instructor cannot access content management")
        void instructorCannotAccessContentManagement() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), any()))
                    .thenReturn(true);

            mockMvc.perform(get("/api/content"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("Admin cannot access content management")
        void adminCannotAccessContentManagement() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), any()))
                    .thenReturn(true);

            mockMvc.perform(get("/api/content"))
                    .andExpect(status().isForbidden());
        }
    }

    // -----------------------------------------------------------------------
    // Media metadata access: either CONTENT_CURATOR+CONTENT_REVIEW or CONTENT_VIEW
    // -----------------------------------------------------------------------
    @Nested
    @DisplayName("Media metadata access (mixed permission path)")
    class MediaMetadataAccess {

        @Test
        @WithMockUser(roles = "CONTENT_CURATOR")
        @DisplayName("Curator with CONTENT_REVIEW can access media metadata")
        void curatorWithReviewCanAccessMetadata() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("CONTENT_REVIEW")))
                    .thenReturn(true);
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("CONTENT_VIEW")))
                    .thenReturn(false);

            when(contentStandardizationService.getMediaMetadata(1L)).thenReturn(List.of());

            mockMvc.perform(get("/api/content/media-metadata/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student with CONTENT_VIEW can access media metadata")
        void studentWithContentViewCanAccessMetadata() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("CONTENT_VIEW")))
                    .thenReturn(true);
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("CONTENT_REVIEW")))
                    .thenReturn(false);

            when(contentStandardizationService.getMediaMetadata(1L)).thenReturn(List.of());

            mockMvc.perform(get("/api/content/media-metadata/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "STUDENT")
        @DisplayName("Student without CONTENT_VIEW cannot access media metadata")
        void studentWithoutContentViewCannotAccessMetadata() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("CONTENT_VIEW")))
                    .thenReturn(false);
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("CONTENT_REVIEW")))
                    .thenReturn(false);

            mockMvc.perform(get("/api/content/media-metadata/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "CONTENT_CURATOR")
        @DisplayName("Curator without either permission cannot access media metadata")
        void curatorWithoutPermissionsCannotAccessMetadata() throws Exception {
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("CONTENT_REVIEW")))
                    .thenReturn(false);
            when(permissionEvaluator.hasPermission(any(Authentication.class), any(), eq("CONTENT_VIEW")))
                    .thenReturn(false);

            mockMvc.perform(get("/api/content/media-metadata/1"))
                    .andExpect(status().isForbidden());
        }
    }
}
