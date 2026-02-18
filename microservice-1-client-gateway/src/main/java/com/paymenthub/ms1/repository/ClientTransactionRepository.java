package com.paymenthub.ms1.repository;

import com.paymenthub.ms1.entity.ClientTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Repository
public interface ClientTransactionRepository 
        extends JpaRepository<ClientTransaction, Long> {

    Optional<ClientTransaction> findByCorrelationId(String correlationId);

    // Fast update - no need to load entire entity
    @Modifying
    @Transactional
    @Query("UPDATE ClientTransaction t SET t.status = :status, " +
           "t.responsePayload = :responsePayload " +
           "WHERE t.correlationId = :correlationId")
    int updateStatusAndResponse(String correlationId, 
                                String status, 
                                String responsePayload);
}