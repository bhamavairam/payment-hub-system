package com.paymenthub.ms2.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SarvatraResponse {
    private String responseCode;
    private String responseMessage;
    private String transactionId;
    private String rrn;  // Retrieval Reference Number
    private String approvalCode;
    private String balance;
    private String encryptedData;
}