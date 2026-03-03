package com.paymenthub.ms1.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "inbound_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InboundTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correlation_id", unique = true, nullable = false)
    private String correlationId;

    @Column(name = "trace_id", nullable = false)
    private String traceId;

    @Column(name = "client_format", nullable = false)
    private String clientFormat;

    @Column(name = "encrypted_payload", columnDefinition = "TEXT", nullable = false)
    private String encryptedPayload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "canonical_message", columnDefinition = "jsonb", nullable = false)
    private JsonNode canonicalMessage;

    @Column(name = "target_gateway")
    private String targetGateway;

    @Column(name = "card_network")
    private String cardNetwork;

    @Column(name = "card_bin")
    private String cardBIN;

    @Column(name = "status", nullable = false)
    private String status;

    // ✅ RESPONSE STORED AS JSONB
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_payload", columnDefinition = "jsonb")
    private JsonNode responsePayload;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}