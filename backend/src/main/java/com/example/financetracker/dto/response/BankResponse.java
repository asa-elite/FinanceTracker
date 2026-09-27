package com.example.financetracker.dto.response;

import com.example.financetracker.model.entity.Bank;

public record BankResponse(
        Long id,
        String name
        ) {
        public BankResponse(Bank bank) {
            this(bank.getId(), bank.getName().toString());
        }
}
