package com.example.financetracker.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record DeleteTransactionBulkRequest(
        @NotNull List<Long> transactionIdList
        ) {
}
