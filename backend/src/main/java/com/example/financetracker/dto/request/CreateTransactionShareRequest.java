package com.example.financetracker.dto.request;

public record CreateTransactionShareRequest(
        Long transaction_id,
        String user_name
) {
}
