package com.scholarops.service;

import com.scholarops.model.dto.CatalogSearchRequest;
import com.scholarops.model.entity.StandardizedContentRecord;
import com.scholarops.exception.ResourceNotFoundException;
import com.scholarops.repository.StandardizedContentRecordRepository;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class CatalogService {
    private final StandardizedContentRecordRepository contentRepository;
    private final EntityManager entityManager;

    public CatalogService(StandardizedContentRecordRepository contentRepository, EntityManager entityManager) {
        this.contentRepository = contentRepository;
        this.entityManager = entityManager;
    }

    public Page<StandardizedContentRecord> search(CatalogSearchRequest request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<StandardizedContentRecord> cq = cb.createQuery(StandardizedContentRecord.class);
        Root<StandardizedContentRecord> root = cq.from(StandardizedContentRecord.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isTrue(root.get("isPublished")));

        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String kw = "%" + request.getKeyword().toLowerCase() + "%";
            predicates.add(cb.or(
                cb.like(cb.lower(root.get("title")), kw),
                cb.like(cb.lower(root.get("description")), kw)
            ));
        }
        if (request.getContentType() != null) {
            predicates.add(cb.equal(root.get("contentType"), request.getContentType()));
        }
        if (request.getMinPrice() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("price"), request.getMinPrice()));
        }
        if (request.getMaxPrice() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("price"), request.getMaxPrice()));
        }
        if (request.getAvailabilityStart() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("availabilityStart"), request.getAvailabilityStart()));
        }
        if (request.getAvailabilityEnd() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("availabilityEnd"), request.getAvailabilityEnd()));
        }

        cq.where(predicates.toArray(new Predicate[0]));

        if ("popularity".equalsIgnoreCase(request.getSortBy())) {
            cq.orderBy("asc".equalsIgnoreCase(request.getSortDirection()) ?
                    cb.asc(root.get("popularityScore")) : cb.desc(root.get("popularityScore")));
        } else {
            cq.orderBy("asc".equalsIgnoreCase(request.getSortDirection()) ?
                    cb.asc(root.get("createdAt")) : cb.desc(root.get("createdAt")));
        }

        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;

        TypedQuery<StandardizedContentRecord> query = entityManager.createQuery(cq);
        query.setFirstResult(page * size);
        query.setMaxResults(size);
        List<StandardizedContentRecord> results = query.getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<StandardizedContentRecord> countRoot = countQuery.from(StandardizedContentRecord.class);
        countQuery.select(cb.count(countRoot));
        List<Predicate> countPredicates = new ArrayList<>();
        countPredicates.add(cb.isTrue(countRoot.get("isPublished")));
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            String kw = "%" + request.getKeyword().toLowerCase() + "%";
            countPredicates.add(cb.or(
                cb.like(cb.lower(countRoot.get("title")), kw),
                cb.like(cb.lower(countRoot.get("description")), kw)
            ));
        }
        if (request.getContentType() != null) {
            countPredicates.add(cb.equal(countRoot.get("contentType"), request.getContentType()));
        }
        if (request.getMinPrice() != null) {
            countPredicates.add(cb.greaterThanOrEqualTo(countRoot.get("price"), request.getMinPrice()));
        }
        if (request.getMaxPrice() != null) {
            countPredicates.add(cb.lessThanOrEqualTo(countRoot.get("price"), request.getMaxPrice()));
        }
        if (request.getAvailabilityStart() != null) {
            countPredicates.add(cb.greaterThanOrEqualTo(countRoot.get("availabilityStart"), request.getAvailabilityStart()));
        }
        if (request.getAvailabilityEnd() != null) {
            countPredicates.add(cb.lessThanOrEqualTo(countRoot.get("availabilityEnd"), request.getAvailabilityEnd()));
        }
        countQuery.where(countPredicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, PageRequest.of(page, size), total);
    }

    public StandardizedContentRecord getContentById(Long id) {
        return contentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Content not found: " + id));
    }

    public StandardizedContentRecord getPublishedContentById(Long id) {
        StandardizedContentRecord record = getContentById(id);
        if (!Boolean.TRUE.equals(record.getIsPublished())) {
            throw new ResourceNotFoundException("Content not found: " + id);
        }
        return record;
    }

    @Transactional
    public void incrementPopularity(Long id) {
        StandardizedContentRecord record = contentRepository.findById(id).orElse(null);
        if (record != null) {
            record.setPopularityScore(record.getPopularityScore() + 1);
            contentRepository.save(record);
        }
    }
}
