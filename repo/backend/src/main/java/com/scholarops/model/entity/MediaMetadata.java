package com.scholarops.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "media_metadata")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MediaMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_record_id", nullable = false)
    private StandardizedContentRecord contentRecord;

    @Column(name = "media_type", nullable = false, length = 50)
    private String mediaType;

    @Column(name = "file_name", length = 500)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "local_path", length = 1000)
    private String localPath;

    private Integer width;
    private Integer height;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(length = 128)
    private String checksum;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
