package com.paymenthub.ms1.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "client_transactions",
       indexes = {
           @Index(name = "idx_correlation_id", columnList = "correlation_id"),
           @Index(name = "idx_status",         columnList = "status"),
           @Index(name = "idx_terminal_id",    columnList = "terminal_id")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correlation_id", unique = true, nullable = false)
    private String correlationId;

    // ISO Field 41
    @Column(name = "terminal_id")
    private String terminalId;

    // ISO Field 36
    @Column(name = "txn_type")
    private String txnType;

    // PENDING → SUCCESS / FAILED / TIMEOUT
    @Column(name = "status")
    private String status;

    // Original encrypted payload from client
    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    // Response from MS2/MS3
    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}