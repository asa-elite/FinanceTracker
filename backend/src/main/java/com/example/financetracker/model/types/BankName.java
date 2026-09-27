package com.example.financetracker.model.types;

public enum BankName {
    CHASE, SOFI, NAVY_FED, FIDELITY;

    public static BankName fromString(String value) {
        value = value.replace(' ', '_');

        for (BankName bankName: BankName.values()) {
            if (bankName.name().equalsIgnoreCase(value)) {
                return bankName;
            }
        }
        return null;
    }
}
