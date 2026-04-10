package com.scholarops.crawler;

import com.scholarops.model.entity.CrawlRun;
import com.scholarops.model.entity.CrawlRuleVersion;
import com.scholarops.model.entity.CrawlSourceProfile;
import com.scholarops.model.entity.EncryptedSourceCredential;
import com.scholarops.repository.CrawlRunRepository;
import com.scholarops.service.ContentStandardizationService;
import com.scholarops.service.EncryptionService;
import com.scholarops.service.RateLimiterService;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class CrawlerEngine {

    private static final Logger logger = LoggerFactory.getLogger(CrawlerEngine.class);

    private final ExtractionPipeline extractionPipeline;
    private final RateLimiterService rateLimiterService;
    private final CrawlRunRepository crawlRunRepository;
    private final ContentStandardizationService contentStandardizationService;
    private final EncryptionService encryptionService;

    @Value("${scholarops.crawl.default-rate-limit-per-minute:30}")
    private int defaultRateLimit;

    public CrawlerEngine(ExtractionPipeline extractionPipeline,
            CrawlRunRepository crawlRunRepository,
            ContentStandardizationService contentStandardizationService,
            RateLimiterService rateLimiterService,
            EncryptionService encryptionService) {
        this.extractionPipeline = extractionPipeline;
        this.rateLimiterService = rateLimiterService;
        this.crawlRunRepository = crawlRunRepository;
        this.contentStandardizationService = contentStandardizationService;
        this.encryptionService = encryptionService;
    }

    @Async("crawlExecutor")
    public void executeCrawl(CrawlRun run, CrawlSourceProfile source, CrawlRuleVersion ruleVersion) {
        logger.info("Starting crawl run {} for source {}", run.getId(), source.getName());

        run.setStatus("RUNNING");
        run.setStartedAt(LocalDateTime.now());
        crawlRunRepository.save(run);

        // Decrypt credentials up front if the source requires authentication
        String decryptedUsername = null;
        String decryptedPassword = null;
        String decryptedApiKey = null;
        if (Boolean.TRUE.equals(source.getRequiresAuth()) && source.getCredential() != null) {
            EncryptedSourceCredential cred = source.getCredential();
            byte[] iv = cred.getEncryptionIv();
            try {
                if (cred.getEncryptedUsername() != null) {
                    decryptedUsername = encryptionService.decrypt(cred.getEncryptedUsername(), iv);
                }
                if (cred.getEncryptedPassword() != null) {
                    decryptedPassword = encryptionService.decrypt(cred.getEncryptedPassword(), iv);
                }
                if (cred.getEncryptedApiKey() != null) {
                    decryptedApiKey = encryptionService.decrypt(cred.getEncryptedApiKey(), iv);
                }
            } catch (Exception e) {
                logger.error("Failed to decrypt credentials for source {}: {}", source.getId(), e.getMessage());
                run.setStatus("FAILED");
                run.setCompletedAt(LocalDateTime.now());
                run.setErrorLog("Credential decryption failed: " + e.getMessage());
                crawlRunRepository.save(run);
                return;
            }
        }

        final String authUsername = decryptedUsername;
        final String authPassword = decryptedPassword;
        final String authApiKey = decryptedApiKey;

        int rateLimit = source.getRateLimitPerMinute() != null ? source.getRateLimitPerMinute() : defaultRateLimit;
        List<String> errors = new ArrayList<>();
        int crawled = 0;
        int failed = 0;
        int extracted = 0;

        try {
            String baseUrl = source.getBaseUrl();
            List<String> pagesToCrawl = discoverPages(baseUrl, authUsername, authPassword, authApiKey);
            run.setTotalPages(pagesToCrawl.size());
            crawlRunRepository.save(run);

            for (String pageUrl : pagesToCrawl) {
                if ("CANCELLED".equals(run.getStatus())) {
                    logger.info("Crawl run {} cancelled", run.getId());
                    break;
                }

                while (!rateLimiterService.acquirePermit(source.getId(), rateLimit)) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                try {
                    String content = fetchPage(pageUrl, authUsername, authPassword, authApiKey);
                    Map<String, Object> data = extractionPipeline.execute(
                            content,
                            ruleVersion.getExtractionMethod(),
                            ruleVersion.getRuleDefinition(),
                            ruleVersion.getFieldMappings()
                    );

                    if (ruleVersion.getTypeValidations() != null) {
                        data = extractionPipeline.validateTypes(data, ruleVersion.getTypeValidations());
                    }

                    if (!data.isEmpty()) {
                        if (data.get("sourceUrl") == null) {
                            data.put("sourceUrl", pageUrl);
                        }
                        contentStandardizationService.standardize(data, "UTC", run, source);
                        extracted++;
                    }
                    crawled++;
                } catch (Exception e) {
                    failed++;
                    errors.add(pageUrl + ": " + e.getMessage());
                    logger.warn("Failed to crawl page {}: {}", pageUrl, e.getMessage());
                }

                run.setPagesCrawled(crawled);
                run.setPagesFailed(failed);
                run.setItemsExtracted(extracted);
                crawlRunRepository.save(run);
            }

            run.setStatus("COMPLETED");
        } catch (Exception e) {
            run.setStatus("FAILED");
            errors.add("Fatal error: " + e.getMessage());
            logger.error("Crawl run {} failed", run.getId(), e);
        }

        run.setCompletedAt(LocalDateTime.now());
        run.setErrorLog(errors.isEmpty() ? null : String.join("\n", errors));
        crawlRunRepository.save(run);
    }

    private List<String> discoverPages(String baseUrl, String username, String password, String apiKey) {
        List<String> pages = new ArrayList<>();
        pages.add(baseUrl);
        try {
            Connection conn = Jsoup.connect(baseUrl)
                    .timeout(10000)
                    .userAgent("ScholarOps-Crawler/1.0");
            applyAuth(conn, username, password, apiKey);
            Document doc = conn.get();
            doc.select("a[href]").forEach(link -> {
                String href = link.absUrl("href");
                if (href.startsWith(baseUrl) && !pages.contains(href)) {
                    pages.add(href);
                }
            });
        } catch (IOException e) {
            logger.warn("Could not discover pages from {}: {}", baseUrl, e.getMessage());
        }
        return pages;
    }

    private String fetchPage(String url, String username, String password, String apiKey) throws IOException {
        Connection conn = Jsoup.connect(url)
                .timeout(15000)
                .userAgent("ScholarOps-Crawler/1.0");
        applyAuth(conn, username, password, apiKey);
        return conn.get().html();
    }

    private void applyAuth(Connection conn, String username, String password, String apiKey) {
        if (apiKey != null && !apiKey.isBlank()) {
            conn.header("Authorization", "Bearer " + apiKey);
        } else if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
            String encoded = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
            conn.header("Authorization", "Basic " + encoded);
        }
    }
}
