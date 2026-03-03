package com.paymenthub.ms1.service;

import com.paymenthub.common.dto.CanonicalMessage;
import com.paymenthub.common.enums.DestinationGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GatewayResolverService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Resolve destination gateway from card BIN
     * Priority: 1. Database lookup, 2. Enum-based logic
     */
    public void resolveDestinationGateway(CanonicalMessage message) {
        
        long startTime = System.nanoTime();
        
        String cardNumber = message.getCardNumber();
        
        if (cardNumber == null || cardNumber.length() < 6) {
            log.warn("⚠️ Invalid card number - cannot resolve gateway");
            message.setTargetGateway("UNKNOWN");
            message.setCardNetwork("UNKNOWN");
            return;
        }

        String bin = cardNumber.substring(0, 6);
        message.setCardBIN(bin);

        // STEP 1: Database lookup
        GatewayInfo info = lookupGatewayFromDatabase(bin);
        
        if (info != null) {
            message.setTargetGateway(info.gateway);
            message.setCardNetwork(info.network);
            
            long duration = (System.nanoTime() - startTime) / 1_000_000;
            log.info("🎯 Gateway resolved from DB: {} → {} ({}ms)", 
                    bin, info.gateway, duration);
            return;
        }

        // STEP 2: Enum-based resolution
        DestinationGateway destGateway = DestinationGateway.fromCardBIN(cardNumber);
        message.setTargetGateway(destGateway.getName());
        
        String network = resolveNetworkName(destGateway);
        message.setCardNetwork(network);

        long duration = (System.nanoTime() - startTime) / 1_000_000;
        log.info("🎯 Gateway resolved from enum: {} → {} ({}ms)", 
                bin, destGateway.getName(), duration);
    }

    private GatewayInfo lookupGatewayFromDatabase(String bin) {
        try {
            String sql = "SELECT destination_gateway, card_network FROM card_bin_routing " +
                        "WHERE ? BETWEEN bin_range_start AND bin_range_end " +
                        "AND is_active = TRUE LIMIT 1";
            
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                GatewayInfo info = new GatewayInfo();
                info.gateway = rs.getString("destination_gateway");
                info.network = rs.getString("card_network");
                return info;
            }, bin);
            
        } catch (Exception e) {
            log.debug("BIN {} not found in database, using fallback", bin);
            return null;
        }
    }

    private String resolveNetworkName(DestinationGateway gateway) {
        switch (gateway) {
            case VISA: return "VISA";
            case MASTERCARD: return "MASTERCARD";
            case RUPAY: return "RUPAY";
            case AMEX: return "AMEX";
            default: return "UNKNOWN";
        }
    }

    private static class GatewayInfo {
        String gateway;
        String network;
    }
}