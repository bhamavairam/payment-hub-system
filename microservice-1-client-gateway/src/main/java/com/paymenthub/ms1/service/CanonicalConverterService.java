package com.paymenthub.ms1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.paymenthub.common.dto.CanonicalMessage;
import com.paymenthub.common.enums.MessageFormat;
import com.paymenthub.common.util.TraceIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class CanonicalConverterService {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * Convert any format (ISO8583/JSON/XML) to Canonical Message
     */
    public CanonicalMessage convert(String plainPayload) {
        
        long startTime = System.nanoTime();
        
        try {
            MessageFormat format = MessageFormat.detect(plainPayload);
            log.debug("📋 Detected format: {}", format);

            CanonicalMessage message;

            switch (format) {
                case ISO8583:
                    message = convertFromISO8583(plainPayload);
                    break;
                case JSON:
                    message = convertFromJSON(plainPayload);
                    break;
                case XML:
                    message = convertFromXML(plainPayload);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported format: " + format);
            }

            message.setOriginalFormat(format.getFormat());
            message.setReceivedTime(LocalDateTime.now());

            if (message.getCorrelationId() == null || message.getCorrelationId().isEmpty()) {
                message.setCorrelationId(TraceIdGenerator.generateCorrelationId());
            }
            if (message.getTraceId() == null || message.getTraceId().isEmpty()) {
                message.setTraceId(TraceIdGenerator.generateTraceId());
            }

            message.setMaskedCardNumber(maskCardNumber(message.getCardNumber()));

            // ════════════════════════════════════════════════════
            // DETERMINE FRAUD & NOTIFICATION FLAGS
            // ════════════════════════════════════════════════════
            setFraudAndNotificationFlags(message);

            long duration = (System.nanoTime() - startTime) / 1_000_000;
            log.debug("✅ Conversion completed in {}ms", duration);

            return message;

        } catch (Exception e) {
            log.error("❌ Conversion failed", e);
            throw new RuntimeException("Failed to convert to canonical format", e);
        }
    }

    /**
     * Convert from ISO8583 format
     */
    private CanonicalMessage convertFromISO8583(String iso) throws Exception {
        log.debug("🔄 Converting from ISO8583...");
        
        // Parse ISO8583 - simplified parsing
        // In production, use jPOS library for proper ISO8583 parsing
        
        Map<String, String> isoFields = parseISO8583(iso);
        
        String mti = isoFields.get("0");
        String cardNumber = isoFields.get("2");
        String processingCode = isoFields.get("3");
        String amount = isoFields.get("4");
        String stan = isoFields.get("11");
        String terminalId = isoFields.get("41");
        String merchantId = isoFields.get("42");

        return CanonicalMessage.builder()
                .mti(mti)
                .cardNumber(cardNumber)
                .processingCode(processingCode)
                .amount(parseAmount(amount))
                .stan(stan)
                .terminalId(terminalId)
                .merchantId(merchantId)
                .txnType(deriveTxnType(mti, processingCode))
                .currency("INR")
                .isoFields(isoFields)
                .transactionTime(LocalDateTime.now())
                .additionalData(new HashMap<>())
                .build();
    }

    /**
     * Parse ISO8583 message (simplified)
     */
    private Map<String, String> parseISO8583(String iso) throws Exception {
        Map<String, String> fields = new HashMap<>();
        
        try {
            // Try parsing as JSON-style ISO (like your sample)
            JsonNode root = objectMapper.readTree(iso);
            
            root.fieldNames().forEachRemaining(fieldName -> {
                fields.put(fieldName, root.get(fieldName).asText());
            });
            
            return fields;
            
        } catch (Exception e) {
            // If JSON parsing fails, try actual ISO8583 binary format
            // This is where you'd use jPOS library in production
            log.warn("⚠️ ISO8583 binary parsing not implemented - treating as JSON");
            throw new RuntimeException("ISO8583 binary parsing not supported yet");
        }
    }

    /**
     * Convert from JSON format
     */
    private CanonicalMessage convertFromJSON(String json) throws Exception {

        JsonNode root = objectMapper.readTree(json);

        // 🔥 Detect ISO-style JSON (field numbers like "0","2","3","4")
        if (root.has("0") || root.has("2")) {
            log.debug("🔄 Detected ISO Field JSON structure");

            String mti = getTextValue(root, "0");
            String cardNumber = getTextValue(root, "2");
            String processingCode = getTextValue(root, "3");
            String amountStr = getTextValue(root, "4");
            String stan = getTextValue(root, "11");
            String terminalId = getTextValue(root, "41");
            String merchantId = getTextValue(root, "42");

            return CanonicalMessage.builder()
                    .mti(mti)
                    .cardNumber(cardNumber)
                    .processingCode(processingCode)
                    .amount(parseAmount(amountStr))
                    .stan(stan)
                    .terminalId(terminalId)
                    .merchantId(merchantId)
                    .txnType(deriveTxnType(mti, processingCode))
                    .currency("INR")
                    .isoFields(convertJsonToMap(root))
                    .transactionTime(LocalDateTime.now())
                    .additionalData(new HashMap<>())
                    .build();
        }

        // Otherwise treat as business JSON
        log.debug("🔄 Detected Business JSON structure");

        return CanonicalMessage.builder()
                .correlationId(getTextValue(root, "correlationId"))
                .txnType(getTextValue(root, "txnType"))
                .mti(getTextValue(root, "mti"))
                .terminalId(getTextValue(root, "terminalId"))
                .merchantId(getTextValue(root, "merchantId"))
                .cardNumber(getTextValue(root, "cardNumber"))
                .amount(getDoubleValue(root, "amount"))
                .currency(getTextValue(root, "currency", "INR"))
                .stan(getTextValue(root, "stan"))
                .processingCode(getTextValue(root, "processingCode"))
                .transactionTime(parseTimestamp(getTextValue(root, "timestamp")))
                .additionalData(new HashMap<>())
                .build();
    }
    
    private Map<String, String> convertJsonToMap(JsonNode root) {
        Map<String, String> map = new HashMap<>();
        root.fieldNames().forEachRemaining(field -> 
            map.put(field, root.get(field).asText())
        );
        return map;
    }
    /**
     * Convert from XML format
     */
    private CanonicalMessage convertFromXML(String xml) throws Exception {
        // Simplified XML parsing
        // In production, use proper XML parser like JAXB or Jackson XML
        log.warn("⚠️ XML parsing simplified - use proper parser in production");
        
        return CanonicalMessage.builder()
                .correlationId(TraceIdGenerator.generateCorrelationId())
                .txnType("UNKNOWN")
                .additionalData(new HashMap<>())
                .build();
    }

    /**
     * Set fraud and notification flags based on transaction type
     */
    private void setFraudAndNotificationFlags(CanonicalMessage message) {
        String txnType = message.getTxnType();
        String mti = message.getMti();

        // ════════════════════════════════════════════════════
        // FRAUD CHECK RULES
        // ════════════════════════════════════════════════════
        // Enable fraud for: PURCHASE, WITHDRAWAL
        // Disable fraud for: BALANCE_INQUIRY, REVERSAL
        
        boolean enableFraud = true;
        boolean enableNotification = true;

        if (txnType != null) {
            switch (txnType.toUpperCase()) {
                case "BALANCE_INQUIRY":
                case "MINI_STATEMENT":
                    enableFraud = false;        // No fraud check for inquiries
                    enableNotification = false; // No notification for inquiries
                    break;
                
                case "REVERSAL":
                case "REFUND":
                    enableFraud = false;        // No fraud check for reversals
                    enableNotification = true;  // But send notification
                    break;
                
                case "PURCHASE":
                case "WITHDRAWAL":
                case "CASH_ADVANCE":
                    enableFraud = true;         // Full fraud check
                    enableNotification = true;  // Send notification
                    break;
                
                default:
                    enableFraud = true;
                    enableNotification = true;
            }
        }

        // Override based on MTI if available
        if (mti != null) {
            if (mti.startsWith("04")) {  // 0400 = Reversal
                enableFraud = false;
                enableNotification = true;
            }
        }

        message.setEnableFraud(enableFraud);
        message.setEnableNotification(enableNotification);

        log.debug("🔍 Fraud: {} | Notification: {} | Type: {}", 
                enableFraud, enableNotification, txnType);
    }

    /**
     * Derive transaction type from MTI and processing code
     */
    private String deriveTxnType(String mti, String processingCode) {
        if (mti == null) return "UNKNOWN";

        // MTI-based detection
        if (mti.equals("0200")) {
            if (processingCode != null) {
                String first2 = processingCode.substring(0, 2);
                switch (first2) {
                    case "00": return "PURCHASE";
                    case "01": return "WITHDRAWAL";
                    case "31": return "BALANCE_INQUIRY";
                    case "38": return "MINI_STATEMENT";
                    default: return "PURCHASE";
                }
            }
            return "PURCHASE";
        }
        
        if (mti.equals("0400")) {
            return "REVERSAL";
        }
        
        if (mti.equals("0420")) {
            return "REVERSAL_ADVICE";
        }

        return "UNKNOWN";
    }

    private String getTextValue(JsonNode node, String fieldName) {
        return node.has(fieldName) ? node.get(fieldName).asText() : null;
    }

    private String getTextValue(JsonNode node, String fieldName, String defaultValue) {
        return node.has(fieldName) ? node.get(fieldName).asText() : defaultValue;
    }

    private Double getDoubleValue(JsonNode node, String fieldName) {
        return node.has(fieldName) ? node.get(fieldName).asDouble() : null;
    }

    private Double parseAmount(String amountStr) {
        if (amountStr == null) return null;
        try {
            // ISO8583 amounts are usually in cents (000000002050 = 20.50)
            long cents = Long.parseLong(amountStr);
            return cents / 100.0;
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseTimestamp(String timestamp) {
        if (timestamp == null) return LocalDateTime.now();
        
        try {
            return LocalDateTime.parse(timestamp, 
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 8) {
            return cardNumber;
        }
        return cardNumber.substring(0, 4) + "****" + 
               cardNumber.substring(cardNumber.length() - 4);
    }
}