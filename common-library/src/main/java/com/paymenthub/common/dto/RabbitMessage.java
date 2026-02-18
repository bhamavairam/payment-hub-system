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
public class RabbitMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // Unique transaction ID (matches request and response)
    private String correlationId;
    
    // PLAIN JSON string of TransactionRequest
    // MS1 → MS2: Plain JSON (no encryption between services)
    private String plainJsonPayload;
    
    // Message timestamp
    private Long timestamp;
}