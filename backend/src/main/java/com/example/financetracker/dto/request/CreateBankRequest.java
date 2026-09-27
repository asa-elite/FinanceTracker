package com.example.financetracker.dto.request;

import com.example.financetracker.model.types.BankName;
import jakarta.validation.constraints.NotBlank;

public record CreateBankRequest(
        @NotBlank BankName name
        ) {
}
