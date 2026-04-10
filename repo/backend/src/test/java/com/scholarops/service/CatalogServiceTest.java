package com.scholarops.service;

import com.scholarops.model.dto.CatalogSearchRequest;
import com.scholarops.model.entity.StandardizedContentRecord;
import com.scholarops.repository.StandardizedContentRecordRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

    @Mock private StandardizedContentRecordRepository contentRepository;
    @Mock private EntityManager entityManager;
    @Mock private CriteriaBuilder criteriaBuilder;
    @Mock private CriteriaQuery<StandardizedContentRecord> criteriaQuery;
    @Mock private CriteriaQuery<Long> countCriteriaQuery;
    @Mock private Root<StandardizedContentRecord> root;
    @Mock private Root<StandardizedContentRecord> countRoot;
    @Mock private TypedQuery<StandardizedContentRecord> typedQuery;
    @Mock private TypedQuery<Long> countTypedQuery;
    @Mock private Path<Object> path;
    @Mock private Predicate predicate;
    @Mock private Order order;

    @InjectMocks
    private CatalogService catalogService;

    @SuppressWarnings("unchecked")
    private void setupMocks() {
        when(entityManager.getCriteriaBuilder()).thenReturn(criteriaBuilder);
        when(criteriaBuilder.createQuery(StandardizedContentRecord.class)).thenReturn(criteriaQuery);
        when(criteriaQuery.from(StandardizedContentRecord.class)).thenReturn(root);
        when(root.get(anyString())).thenReturn(path);
        when(criteriaBuilder.isTrue(any())).thenReturn(predicate);
        when(criteriaQuery.where(any(Predicate[].class))).thenReturn(criteriaQuery);
        when(criteriaBuilder.desc(any())).thenReturn(order);
        when(criteriaQuery.orderBy(any(Order.class))).thenReturn(criteriaQuery);
        when(entityManager.createQuery(criteriaQuery)).thenReturn(typedQuery);
        when(typedQuery.setFirstResult(anyInt())).thenReturn(typedQuery);
        when(typedQuery.setMaxResults(anyInt())).thenReturn(typedQuery);

        when(criteriaBuilder.createQuery(Long.class)).thenReturn(countCriteriaQuery);
        when(countCriteriaQuery.from(StandardizedContentRecord.class)).thenReturn(countRoot);
        when(countRoot.get(anyString())).thenReturn(path);
        when(criteriaBuilder.count(any())).thenReturn(null);
        when(countCriteriaQuery.select(any())).thenReturn(countCriteriaQuery);
        when(countCriteriaQuery.where(any(Predicate[].class))).thenReturn(countCriteriaQuery);
        when(entityManager.createQuery(countCriteriaQuery)).thenReturn(countTypedQuery);
        when(countTypedQuery.getSingleResult()).thenReturn(0L);
    }

    @Test
    void testSearchByKeyword() {
        setupMocks();
        when(criteriaBuilder.or(any(), any())).thenReturn(predicate);
        when(criteriaBuilder.like(any(), anyString())).thenReturn(predicate);
        when(criteriaBuilder.lower(any())).thenReturn(null);
        when(typedQuery.getResultList()).thenReturn(List.of());
        when(countTypedQuery.getSingleResult()).thenReturn(0L);

        CatalogSearchRequest request = CatalogSearchRequest.builder()
                .keyword("math").page(0).size(20).build();

        Page<StandardizedContentRecord> result = catalogService.search(request);

        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void testSearchByPriceRange() {
        setupMocks();
        when(criteriaBuilder.greaterThanOrEqualTo(any(), any(BigDecimal.class))).thenReturn(predicate);
        when(criteriaBuilder.lessThanOrEqualTo(any(), any(BigDecimal.class))).thenReturn(predicate);
        when(typedQuery.getResultList()).thenReturn(List.of());

        CatalogSearchRequest request = CatalogSearchRequest.builder()
                .minPrice(BigDecimal.valueOf(10)).maxPrice(BigDecimal.valueOf(50))
                .page(0).size(20).build();

        Page<StandardizedContentRecord> result = catalogService.search(request);

        assertNotNull(result);
    }

    @Test
    void testSortByPopularity() {
        setupMocks();
        when(typedQuery.getResultList()).thenReturn(List.of());

        CatalogSearchRequest request = CatalogSearchRequest.builder()
                .sortBy("popularity").sortDirection("desc").page(0).size(20).build();

        Page<StandardizedContentRecord> result = catalogService.search(request);

        assertNotNull(result);
        verify(criteriaBuilder).desc(any());
    }

    @Test
    void testSortByNewest() {
        setupMocks();
        when(typedQuery.getResultList()).thenReturn(List.of());

        CatalogSearchRequest request = CatalogSearchRequest.builder()
                .sortBy("newest").sortDirection("desc").page(0).size(20).build();

        Page<StandardizedContentRecord> result = catalogService.search(request);

        assertNotNull(result);
    }

    @Test
    void testPagination() {
        setupMocks();
        when(typedQuery.getResultList()).thenReturn(List.of());
        when(countTypedQuery.getSingleResult()).thenReturn(100L);

        CatalogSearchRequest request = CatalogSearchRequest.builder()
                .page(2).size(10).build();

        Page<StandardizedContentRecord> result = catalogService.search(request);

        assertNotNull(result);
        assertEquals(100L, result.getTotalElements());
        verify(typedQuery).setFirstResult(20);
        verify(typedQuery).setMaxResults(10);
    }
}
