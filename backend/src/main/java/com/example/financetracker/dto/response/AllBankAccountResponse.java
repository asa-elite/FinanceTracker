package com.example.financetracker.dto.response;

import java.util.List;

public record AllBankAccountResponse(
        Long bankId,
        String bankName,
        List<BankAccountResponse> accounts
) {
}
