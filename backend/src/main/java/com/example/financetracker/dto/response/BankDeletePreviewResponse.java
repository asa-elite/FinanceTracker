package com.example.financetracker.dto.response;

public record BankDeletePreviewResponse(
        Long bankId,
        String bankName,
        int accountsDeleted,
        int transactionsDeleted,
        int transactionSplitsDeleted,
        int sharedTransactionsAffected
) {
}
