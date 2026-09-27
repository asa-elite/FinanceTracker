package com.example.financetracker.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CreateTransactionRequest(
        @NotNull BigDecimal totalAmount,
        String description,
        String merchant,
        LocalDateTime transactionDate,
        List<CreateTransactionSplitRequest> splits,
        Long bankId
) {
}
