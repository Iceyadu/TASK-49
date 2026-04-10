package com.scholarops.repository;

import com.scholarops.model.entity.MediaMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaMetadataRepository extends JpaRepository<MediaMetadata, Long> {

    List<MediaMetadata> findByContentRecordId(Long contentRecordId);
}
