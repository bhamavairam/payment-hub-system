package com.paymenthub.ms1.repository;

import com.paymenthub.ms1.entity.ClientEncryptionKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientEncryptionKeyRepository extends JpaRepository<ClientEncryptionKey, Long> {
    
    Optional<ClientEncryptionKey> findByClientIdAndIsActive(String clientId, Boolean isActive);
    
    Optional<ClientEncryptionKey> findByClientId(String clientId);
    
    // ✅ NEW: Find all active clients for warmup
    List<ClientEncryptionKey> findAllByIsActive(Boolean isActive);
}