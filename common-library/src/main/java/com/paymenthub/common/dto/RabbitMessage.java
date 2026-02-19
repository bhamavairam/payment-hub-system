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

    private String correlationId;
    private String plainJsonPayload;
    private String source;       // ← NEW: e.g. "TERMINAL-001", "BANK001"
    private String destination;  // ← NEW: "NPCI" → MS2, "VISA" → MS3
    private Long timestamp;
}