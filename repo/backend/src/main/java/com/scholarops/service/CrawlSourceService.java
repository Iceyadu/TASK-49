package com.scholarops.service;

import com.scholarops.exception.ForbiddenException;
import com.scholarops.exception.ResourceNotFoundException;
import com.scholarops.model.dto.CrawlSourceRequest;
import com.scholarops.model.entity.CrawlSourceProfile;
import com.scholarops.model.entity.EncryptedSourceCredential;
import com.scholarops.model.entity.User;
import com.scholarops.model.enums.AuditAction;
import com.scholarops.repository.CrawlSourceProfileRepository;
import com.scholarops.repository.EncryptedSourceCredentialRepository;
import com.scholarops.util.AesEncryptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class CrawlSourceService {

    private static final Logger logger = LoggerFactory.getLogger(CrawlSourceService.class);

    private final CrawlSourceProfileRepository crawlSourceProfileRepository;
    private final EncryptedSourceCredentialRepository encryptedSourceCredentialRepository;
    private final EncryptionService encryptionService;
    private final AuditLogService auditLogService;

    public CrawlSourceService(CrawlSourceProfileRepository crawlSourceProfileRepository,
                               EncryptedSourceCredentialRepository encryptedSourceCredentialRepository,
                               EncryptionService encryptionService,
                               AuditLogService auditLogService) {
        this.crawlSourceProfileRepository = crawlSourceProfileRepository;
        this.encryptedSourceCredentialRepository = encryptedSourceCredentialRepository;
        this.encryptionService = encryptionService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public CrawlSourceProfile createSource(CrawlSourceRequest request, Long userId) {
        CrawlSourceProfile profile = CrawlSourceProfile.builder()
                .name(request.getName())
                .baseUrl(request.getBaseUrl())
                .description(request.getDescription())
                .rateLimitPerMinute(request.getRateLimitPerMinute() != null ? request.getRateLimitPerMinute() : 30)
                .requiresAuth(request.getRequiresAuth() != null ? request.getRequiresAuth() : false)
                .enabled(true)
                .createdBy(User.builder().id(userId).build())
                .build();

        CrawlSourceProfile savedProfile = crawlSourceProfileRepository.save(profile);

        // Encrypt and store credentials if provided
        if (hasCredentials(request)) {
            encryptAndSaveCredentials(savedProfile, request);
        }

        auditLogService.log(
                userId,
                AuditAction.CRAWL_SOURCE_CREATE,
                "CrawlSourceProfile",
                savedProfile.getId(),
                "Created crawl source: " + savedProfile.getName(),
                null,
                null
        );

        logger.info("Crawl source '{}' created by userId={}", savedProfile.getName(), userId);
        return savedProfile;
    }

    @Transactional
    public CrawlSourceProfile updateSource(Long id, CrawlSourceRequest request, Long userId) {
        CrawlSourceProfile profile = crawlSourceProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CrawlSourceProfile", "id", id));

        verifyOwnership(profile, userId);

        profile.setName(request.getName());
        profile.setBaseUrl(request.getBaseUrl());
        profile.setDescription(request.getDescription());
        if (request.getRateLimitPerMinute() != null) {
            profile.setRateLimitPerMinute(request.getRateLimitPerMinute());
        }
        if (request.getRequiresAuth() != null) {
            profile.setRequiresAuth(request.getRequiresAuth());
        }

        // Re-encrypt credentials if provided
        if (hasCredentials(request)) {
            // Remove existing credentials if present
            if (profile.getCredential() != null) {
                encryptedSourceCredentialRepository.delete(profile.getCredential());
                profile.setCredential(null);
            }
            encryptAndSaveCredentials(profile, request);
        }

        CrawlSourceProfile updatedProfile = crawlSourceProfileRepository.save(profile);

        auditLogService.log(
                userId,
                AuditAction.CRAWL_SOURCE_UPDATE,
                "CrawlSourceProfile",
                id,
                "Updated crawl source: " + profile.getName(),
                null,
                null
        );

        logger.info("Crawl source '{}' updated by userId={}", profile.getName(), userId);
        return updatedProfile;
    }

    @Transactional(readOnly = true)
    public CrawlSourceProfile getSource(Long id, Long userId) {
        CrawlSourceProfile profile = crawlSourceProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CrawlSourceProfile", "id", id));

        verifyOwnership(profile, userId);
        return profile;
    }

    public Map<String, String> getDecryptedCredentials(Long sourceId, Long userId) {
        CrawlSourceProfile profile = getSource(sourceId, userId);
        EncryptedSourceCredential cred = profile.getCredential();
        Map<String, String> result = new java.util.HashMap<>();
        if (cred == null) {
            return result;
        }
        byte[] iv = cred.getEncryptionIv();
        if (cred.getEncryptedUsername() != null) {
            result.put("username", encryptionService.decrypt(cred.getEncryptedUsername(), iv));
        }
        if (cred.getEncryptedPassword() != null) {
            result.put("password", encryptionService.decrypt(cred.getEncryptedPassword(), iv));
        }
        if (cred.getEncryptedApiKey() != null) {
            result.put("apiKey", encryptionService.decrypt(cred.getEncryptedApiKey(), iv));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<CrawlSourceProfile> listSources(Long userId) {
        return crawlSourceProfileRepository.findByCreatedById(userId);
    }

    @Transactional
    public void deleteSource(Long id, Long userId) {
        CrawlSourceProfile profile = crawlSourceProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CrawlSourceProfile", "id", id));

        verifyOwnership(profile, userId);

        String name = profile.getName();
        crawlSourceProfileRepository.delete(profile);

        auditLogService.log(
                userId,
                AuditAction.CRAWL_SOURCE_DELETE,
                "CrawlSourceProfile",
                id,
                "Deleted crawl source: " + name,
                null,
                null
        );

        logger.info("Crawl source '{}' deleted by userId={}", name, userId);
    }

    private void verifyOwnership(CrawlSourceProfile profile, Long userId) {
        if (!profile.getCreatedBy().getId().equals(userId)) {
            throw new ForbiddenException("You do not have permission to access this crawl source");
        }
    }

    private boolean hasCredentials(CrawlSourceRequest request) {
        return (request.getUsername() != null && !request.getUsername().isBlank())
                || (request.getPassword() != null && !request.getPassword().isBlank())
                || (request.getApiKey() != null && !request.getApiKey().isBlank());
    }

    private void encryptAndSaveCredentials(CrawlSourceProfile profile, CrawlSourceRequest request) {
        // Use a single IV for all credential fields of this source
        byte[] iv = null;
        byte[] encryptedUsername = null;
        byte[] encryptedPassword = null;
        byte[] encryptedApiKey = null;

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            AesEncryptionUtil.EncryptedData data = encryptionService.encrypt(request.getUsername());
            encryptedUsername = data.getCiphertext();
            iv = data.getIv();
        }

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            AesEncryptionUtil.EncryptedData data = encryptionService.encrypt(request.getPassword());
            encryptedPassword = data.getCiphertext();
            if (iv == null) {
                iv = data.getIv();
            }
        }

        if (request.getApiKey() != null && !request.getApiKey().isBlank()) {
            AesEncryptionUtil.EncryptedData data = encryptionService.encrypt(request.getApiKey());
            encryptedApiKey = data.getCiphertext();
            if (iv == null) {
                iv = data.getIv();
            }
        }

        if (iv != null) {
            EncryptedSourceCredential credential = EncryptedSourceCredential.builder()
                    .sourceProfile(profile)
                    .encryptedUsername(encryptedUsername)
                    .encryptedPassword(encryptedPassword)
                    .encryptedApiKey(encryptedApiKey)
                    .encryptionIv(iv)
                    .build();
            encryptedSourceCredentialRepository.save(credential);
            profile.setCredential(credential);
        }
    }
}
