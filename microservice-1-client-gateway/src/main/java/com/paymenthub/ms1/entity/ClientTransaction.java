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
           @Index(name = "idx_status", columnList = "status"),
           @Index(name = "idx_terminal_id", columnList = "terminal_id")
       })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique transaction ID (links request + response)
    @Column(name = "correlation_id", unique = true, nullable = false)
    private String correlationId;

    // Which ATM/POS sent this
    @Column(name = "terminal_id")
    private String terminalId;

    // WITHDRAWAL, PURCHASE, BALANCE_INQUIRY
    @Column(name = "txn_type")
    private String txnType;

    // Transaction amount
    @Column(name = "amount")
    private Double amount;

    // Masked card: 4111****1111
    @Column(name = "card_number")
    private String cardNumber;

    // PENDING → SUCCESS or FAILED
    @Column(name = "status")
    private String status;

    // Original encrypted request from terminal
    @Column(name = "request_payload", columnDefinition = "TEXT")
    private String requestPayload;

    // Final response from Sarvatra
    @Column(name = "response_payload", columnDefinition = "TEXT")
    private String responsePayload;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}