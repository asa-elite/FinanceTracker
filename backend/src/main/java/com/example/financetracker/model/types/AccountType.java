package com.example.financetracker.model.types;

public enum AccountType {
    CHECKING, SAVING, CREDIT_CARD;

    public static AccountType fromString(String value) {
        value = value.replace(' ', '_');

        for (AccountType type: AccountType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        return null;
    }
}
