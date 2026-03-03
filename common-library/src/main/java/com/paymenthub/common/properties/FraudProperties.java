package com.paymenthub.common.properties;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class FraudProperties {
    
    private boolean enabled = true;
    private long timeoutMs = 5000;
    private String defaultDecision = "APPROVED";
    
    // Transaction exemptions
    private List<String> exemptTransactions = new ArrayList<>();
    
    // Gateway exemptions
    private List<String> exemptGateways = new ArrayList<>();
    
    // Amount threshold
    private double exemptBelowAmount = 0.0;
    
    // Scoring thresholds
    private int approveBelow = 30;
    private int reviewBelow = 70;
    
    public FraudProperties() {
    }
    
    /**
     * Check if transaction type is exempt from fraud
     */
    public boolean isTransactionExempt(String txnType) {
        return exemptTransactions.contains(txnType);
    }
    
    /**
     * Check if gateway is exempt from fraud
     */
    public boolean isGatewayExempt(String gateway) {
        return exemptGateways.contains(gateway);
    }
    
    /**
     * Check if amount is below exemption threshold
     */
    public boolean isAmountExempt(Double amount) {
        return amount != null && amount < exemptBelowAmount;
    }
}