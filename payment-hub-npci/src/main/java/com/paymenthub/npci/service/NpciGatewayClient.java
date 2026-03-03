package com.paymenthub.npci.service;

import com.paymenthub.common.dto.CanonicalMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NpciGatewayClient {

    public String transform(CanonicalMessage message) {

        // TODO: Proper ISO8583 mapping
        return "0200|" + message.getAmount();
    }

    public String encrypt(String iso) {

        // TODO: Implement Sarvatra AES encryption
        return "ENCRYPTED(" + iso + ")";
    }

    public String callGateway(String encrypted) {

        try {
            Thread.sleep(70); // simulate NPCI network
        } catch (InterruptedException ignored) {}

        return "NPCI_APPROVED";
    }
}