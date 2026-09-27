package com.example.financetracker.dto.result;

import com.example.financetracker.dto.request.CreateTransactionRequest;
import com.example.financetracker.dto.response.TransactionResponse;
import com.example.financetracker.model.entity.Transactions;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public record StatementParseResult(
        LocalDate statementStartDate,
        LocalDate statementEndDate,
        List<CreateTransactionRequest> transactions
) {
}
