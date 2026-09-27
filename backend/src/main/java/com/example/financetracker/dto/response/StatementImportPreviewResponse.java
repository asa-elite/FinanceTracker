package com.example.financetracker.dto.response;

import com.example.financetracker.dto.request.CreateTransactionRequest;
import com.example.financetracker.dto.result.StatementParseResult;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;

public record StatementImportPreviewResponse(
        @NotBlank Long bankAccountId,
        @NotBlank Map<String, LocalDate> statementPeriod,
        @NotBlank List<CreateTransactionRequest> transactions
) {
    public StatementImportPreviewResponse(StatementParseResult result, Long bankAccountId) {
        this(
                bankAccountId,
                Map.of("startDate", result.statementStartDate(), "endDate", result.statementEndDate()),
                result.transactions()
                );
    }
}
