package com.paymenthub.common.dto;

import lombok.Data;

@Data
public class FraudDecision {

    private String correlationId;
    private String decision;   // APPROVED / DECLINED
    private int score;
    private String reason;
}