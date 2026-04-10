package com.scholarops.repository;

import com.scholarops.model.entity.EncryptedSourceCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EncryptedSourceCredentialRepository extends JpaRepository<EncryptedSourceCredential, Long> {

    Optional<EncryptedSourceCredential> findBySourceProfileId(Long sourceProfileId);
}
