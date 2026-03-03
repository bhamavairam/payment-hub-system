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
public class TransactionResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;

    // Tracking
    private String correlationId;
    private String traceId;
    private String transactionId;
    
    // Response details
    private String status;              // SUCCESS, FAILED, PENDING
    private String responseCode;        // 00, 91, etc.
    private String responseMessage;
    
    // Gateway response
    private String gatewayResponseCode;
    private String gatewayMessage;
    private String rrn;                 // Retrieval Reference Number
    private String approvalCode;
    private String balance;             // ✅ Your NPCI code uses this!
    
    // Fraud check result
    private String fraudStatus;         // PASS, FAIL, PENDING
    private Double fraudScore;
    
    // Timestamps
    private LocalDateTime processedAt;
    private Long timestamp;
    
    // Additional data
    @Builder.Default
    private Map<String, Object> additionalData = new HashMap<>();
}