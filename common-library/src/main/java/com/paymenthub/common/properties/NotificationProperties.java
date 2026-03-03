package com.paymenthub.common.properties;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class NotificationProperties {
    
    private boolean enabled = true;
    
    // Transaction exemptions
    private List<String> exemptTransactions = new ArrayList<>();
    
    // Gateway exemptions
    private List<String> exemptGateways = new ArrayList<>();
    
    public NotificationProperties() {
    }
    
    public boolean isTransactionExempt(String txnType) {
        return exemptTransactions.contains(txnType);
    }
    
    public boolean isGatewayExempt(String gateway) {
        return exemptGateways.contains(gateway);
    }
}