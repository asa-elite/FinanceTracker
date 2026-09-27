package com.example.financetracker.dto.response;

import com.example.financetracker.model.types.AccountType;
import com.example.financetracker.model.entity.BankAccount;

import java.math.BigDecimal;

public record BankAccountResponse(
        String account_name,
        AccountType account_type,
        BigDecimal balance,
        Long bank_account_id
) {
    public BankAccountResponse(BankAccount account) {
        this(account.getAccountName(), account.getAccountType(), account.getBalance(), account.getId());
    }
}
