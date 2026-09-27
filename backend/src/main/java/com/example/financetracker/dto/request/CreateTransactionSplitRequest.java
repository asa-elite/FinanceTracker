package com.example.financetracker.dto.request;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateTransactionSplitRequest(
        @NotNull BigDecimal amount,
        @NotNull CreateCategoryRequest category
        ) {
}
