package com.example.financetracker.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record Register(
        @Email @NotBlank String email,
        @NotBlank String password
) {
}
