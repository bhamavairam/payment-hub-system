package com.paymenthub.ms1.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.paymenthub.ms1.entity.InboundTransaction;
import org.springframework.data.jpa.repository.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface InboundTransactionRepository
        extends JpaRepository<InboundTransaction, Long> {

    Optional<InboundTransaction> findByCorrelationId(String correlationId);

    @Modifying
    @Transactional
    @Query("""
        UPDATE InboundTransaction t
        SET t.status = :status,
            t.responsePayload = :responsePayload
        WHERE t.correlationId = :correlationId
    """)
    int updateStatusAndResponse(
            String correlationId,
            String status,
            JsonNode responsePayload
    );
}