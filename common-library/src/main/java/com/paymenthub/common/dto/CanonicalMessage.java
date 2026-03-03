package com.paymenthub.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanonicalMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;

    // ═══════════════════════════════════════════════════════
    // TRACKING
    // ═══════════════════════════════════════════════════════
    private String correlationId;
    private String traceId;
    private String originalFormat;     // ISO8583, JSON, XML

    // ═══════════════════════════════════════════════════════
    // ROUTING
    // ═══════════════════════════════════════════════════════
    private String targetGateway;      // NPCI, VISA, MASTERCARD
    private String cardNetwork;        // VISA, MASTERCARD, RUPAY
    private String cardBIN;            // First 6 digits

    // ═══════════════════════════════════════════════════════
    // FRAUD & NOTIFICATION FLAGS (NEW!)
    // ═══════════════════════════════════════════════════════
    @Builder.Default
    private Boolean enableFraud = true;        // Default: fraud check enabled
    
    @Builder.Default
    private Boolean enableNotification = true;  // Default: notification enabled

    // ═══════════════════════════════════════════════════════
    // TRANSACTION DETAILS
    // ═══════════════════════════════════════════════════════
    private String txnType;            // PURCHASE, WITHDRAWAL, BALANCE_INQUIRY
    private String mti;                // ISO8583 Message Type Indicator (0200, 0400, etc.)
    private String terminalId;
    private String merchantId;
    private String cardNumber;
    private String maskedCardNumber;
    private Double amount;
    private String currency;
    private String stan;               // System Trace Audit Number (field 11)
    private String processingCode;     // ISO field 3
    
    // ═══════════════════════════════════════════════════════
    // TIMESTAMPS
    // ═══════════════════════════════════════════════════════
    private LocalDateTime transactionTime;
    private LocalDateTime receivedTime;

    // ═══════════════════════════════════════════════════════
    // ISO8583 FIELDS (for full ISO support)
    // ═══════════════════════════════════════════════════════
    @Builder.Default
    private Map<String, String> isoFields = new HashMap<>();  // Store all ISO fields

    // ═══════════════════════════════════════════════════════
    // ADDITIONAL DATA (flexible)
    // ═══════════════════════════════════════════════════════
    @Builder.Default
    private Map<String, Object> additionalData = new HashMap<>();
}