package com.paymenthub.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class TraceIdGenerator {

    private static final DateTimeFormatter FORMATTER = 
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Generate unique Correlation ID
     * Format: TXN-YYYYMMDDHHMMSS-XXXX
     */
    public static String generateCorrelationId() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "TXN-" + timestamp + "-" + random;
    }

    /**
     * Generate unique Trace ID for distributed tracing
     * Format: TRC-YYYYMMDDHHMMSS-XXXX
     */
    public static String generateTraceId() {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String random = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "TRC-" + timestamp + "-" + random;
    }
}