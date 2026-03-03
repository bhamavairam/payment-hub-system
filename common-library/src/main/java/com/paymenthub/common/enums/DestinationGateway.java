package com.paymenthub.common.enums;

public enum DestinationGateway {
    NPCI("NPCI"),
    VISA("VISA"),
    MASTERCARD("MASTERCARD"),
    RUPAY("RUPAY"),
    AMEX("AMEX"),
    UNKNOWN("UNKNOWN");

    private final String name;

    DestinationGateway(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static DestinationGateway fromCardBIN(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 6) {
            return UNKNOWN;
        }

        String bin = cardNumber.substring(0, 6);
        int binInt = Integer.parseInt(bin);

        // VISA: 4xxxxx
        if (binInt >= 400000 && binInt <= 499999) {
            return VISA;
        }

        // Mastercard: 51-55xxxx or 2221-2720xx
        if ((binInt >= 510000 && binInt <= 559999) ||
            (binInt >= 222100 && binInt <= 272099)) {
            return MASTERCARD;
        }

        // RuPay: 6xxxxx, 817xxx
        if ((binInt >= 600000 && binInt <= 699999) ||
            (binInt >= 817000 && binInt <= 817999)) {
            return RUPAY;
        }

        // Amex: 34xxxx, 37xxxx
        if (binInt >= 340000 && binInt <= 379999) {
            return AMEX;
        }

        return UNKNOWN;
    }
}