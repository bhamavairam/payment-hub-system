package com.paymenthub.common.enums;

public enum MessageFormat {
    ISO8583("ISO8583"),
    JSON("JSON"),
    XML("XML"),
    UNKNOWN("UNKNOWN");

    private final String format;

    MessageFormat(String format) {
        this.format = format;
    }

    public String getFormat() {
        return format;
    }

    public static MessageFormat detect(String payload) {
        if (payload == null) return UNKNOWN;
        
        payload = payload.trim();
        
        if (payload.startsWith("{") || payload.startsWith("[")) {
            return JSON;
        }
        if (payload.startsWith("<")) {
            return XML;
        }
        if (payload.matches("^\\d{4}.*")) {  // ISO8583 starts with MTI (4 digits)
            return ISO8583;
        }
        
        return UNKNOWN;
    }
}