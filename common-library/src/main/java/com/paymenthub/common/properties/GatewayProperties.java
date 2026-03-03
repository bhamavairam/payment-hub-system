package com.paymenthub.common.properties;

import lombok.Data;

/**
 * Framework-neutral gateway configuration.
 * Pure POJO - no Spring.
 */
@Data
public class GatewayProperties {
    
    private String name;              // NPCI, VISA, MASTERCARD
    private String url;
    private int timeout = 25000;
    private int retryAttempts = 2;
    private long retryDelay = 1000;
    private String apiCode;
    
    public GatewayProperties() {
    }
}