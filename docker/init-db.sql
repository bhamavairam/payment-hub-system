-- ═══════════════════════════════════════════════════════
-- FRAUD & NOTIFICATION ROUTING RULES
-- ═══════════════════════════════════════════════════════

CREATE TABLE IF NOT EXISTS fraud_notification_rules (
    id SERIAL PRIMARY KEY,
    
    -- Rule identification
    rule_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    
    -- Conditions (when to apply this rule)
    destination_gateway VARCHAR(50),  -- NPCI, VISA, MASTERCARD, NULL=all
    txn_type VARCHAR(50),             -- PURCHASE, WITHDRAWAL, etc, NULL=all
    min_amount DECIMAL(15,2),         -- NULL=no min
    max_amount DECIMAL(15,2),         -- NULL=no max
    
    -- Actions (what to do when rule matches)
    enable_fraud BOOLEAN NOT NULL DEFAULT TRUE,
    enable_notification BOOLEAN NOT NULL DEFAULT TRUE,
    
    -- Rule priority (lower number = higher priority)
    priority INT NOT NULL DEFAULT 100,
    
    -- Status
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_fraud_notification_rules_gateway 
    ON fraud_notification_rules(destination_gateway);
CREATE INDEX idx_fraud_notification_rules_active 
    ON fraud_notification_rules(is_active);
CREATE INDEX idx_fraud_notification_rules_priority 
    ON fraud_notification_rules(priority);

-- ═══════════════════════════════════════════════════════
-- SAMPLE RULES
-- ═══════════════════════════════════════════════════════

-- Rule 1: Disable fraud for small NPCI transactions (< 100)
INSERT INTO fraud_notification_rules 
(rule_name, description, destination_gateway, max_amount, enable_fraud, enable_notification, priority) 
VALUES 
('npci_small_txn_no_fraud', 
 'Skip fraud check for NPCI transactions under 100', 
 'NPCI', 100.00, FALSE, TRUE, 10);

-- Rule 2: Disable notification for VISA balance inquiries
INSERT INTO fraud_notification_rules 
(rule_name, description, destination_gateway, txn_type, enable_fraud, enable_notification, priority) 
VALUES 
('visa_balance_inquiry_no_notif', 
 'Skip notification for VISA balance inquiries', 
 'VISA', 'BALANCE_INQUIRY', TRUE, FALSE, 20);

-- Rule 3: Enable both for high-value transactions (> 10000)
INSERT INTO fraud_notification_rules 
(rule_name, description, min_amount, enable_fraud, enable_notification, priority) 
VALUES 
('high_value_full_check', 
 'Full fraud and notification for high-value transactions', 
 10000.00, TRUE, TRUE, 5);

-- Rule 4: Disable both for internal test transactions
INSERT INTO fraud_notification_rules 
(rule_name, description, txn_type, enable_fraud, enable_notification, priority) 
VALUES 
('test_txn_no_checks', 
 'Skip all checks for test transactions', 
 'TEST', FALSE, FALSE, 1);

-- Rule 5: Default rule - enable everything (lowest priority)
INSERT INTO fraud_notification_rules 
(rule_name, description, enable_fraud, enable_notification, priority) 
VALUES 
('default_rule', 
 'Default: enable fraud and notification', 
 TRUE, TRUE, 999);