package com.paymenthub.ms2.dto;

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
    private String correlationId;
    private String status;  // SUCCESS, FAILED, TIMEOUT
    private String responseCode;
    private String responseMessage;
    private String transactionId;
    private String rrn;
    private String approvalCode;
    private Long timestamp;
}