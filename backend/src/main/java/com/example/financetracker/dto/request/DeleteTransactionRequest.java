package com.example.financetracker.dto.request;

import jakarta.validation.constraints.NotNull;

public record DeleteTransactionRequest(
        @NotNull Long transactionId
) {
}
