package com.example.financetracker.dto.request;

import java.time.LocalDateTime;

public record ListTransactionRequest(
        LocalDateTime start,
        LocalDateTime end,
        Integer days
) {
}