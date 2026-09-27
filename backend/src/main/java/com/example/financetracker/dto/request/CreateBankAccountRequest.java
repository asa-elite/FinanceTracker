package com.example.financetracker.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateBankAccountRequest(
        @NotNull
        String name,
        @NotNull
        String type,
        @NotNull
        long bankId
) {
}
