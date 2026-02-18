package com.paymenthub.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    // Matches the request correlationId
    private String correlationId;

    // SUCCESS or FAILED or TIMEOUT
    private String status;

    // 00 = Approved, 51 = Insufficient funds, etc.
    private String responseCode;

    // Human readable message for terminal display
    private String responseMessage;

    // Sarvatra's transaction reference
    private String transactionId;

    // Retrieval Reference Number (printed on receipt)
    private String rrn;

    // Approval code (printed on receipt)
    private String approvalCode;

    // Remaining balance (for balance inquiry)
    private String balance;

    // When response was generated
    private Long timestamp;
}