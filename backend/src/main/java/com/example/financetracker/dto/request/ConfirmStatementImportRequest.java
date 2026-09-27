package com.example.financetracker.dto.request;

import com.example.financetracker.dto.response.TransactionResponse;

import java.time.LocalDate;
import java.util.List;

public record ConfirmStatementImportRequest(
        Long bankAccountId,
        LocalDate statementStartDate,
        LocalDate statementEndDate,
        List<TransactionResponse> transactions
) {}
