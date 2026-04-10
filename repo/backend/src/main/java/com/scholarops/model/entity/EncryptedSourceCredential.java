package com.scholarops.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "encrypted_source_credentials")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EncryptedSourceCredential {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_profile_id", nullable = false, unique = true)
    private CrawlSourceProfile sourceProfile;

    @JsonIgnore
    @Column(name = "encrypted_username", columnDefinition = "VARBINARY(512)")
    private byte[] encryptedUsername;

    @JsonIgnore
    @Column(name = "encrypted_password", columnDefinition = "VARBINARY(512)")
    private byte[] encryptedPassword;

    @JsonIgnore
    @Column(name = "encrypted_api_key", columnDefinition = "VARBINARY(512)")
    private byte[] encryptedApiKey;

    @JsonIgnore
    @Column(name = "encryption_iv", nullable = false, columnDefinition = "VARBINARY(16)")
    private byte[] encryptionIv;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
