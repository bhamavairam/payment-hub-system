package com.paymenthub.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    // Unique ID - ties request and response together
    private String correlationId;

    // ATM or POS terminal ID
    private String terminalId;

    // WITHDRAWAL, PURCHASE, BALANCE_INQUIRY
    private String txnType;

    // Transaction amount
    private Double amount;

    // Card number (will be masked in DB)
    private String cardNumber;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
}