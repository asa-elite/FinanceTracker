package com.example.financetracker.dto.request;

import jakarta.validation.constraints.NotBlank;

public record DeleteBankRequest(
        @NotBlank Long bankId
) {
}
