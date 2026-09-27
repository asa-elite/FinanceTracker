package com.example.financetracker.dto.response;

import com.example.financetracker.model.entity.Transactions;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TransactionResponse(
        Long transaction_id,
        LocalDateTime transaction_date,
        BigDecimal total_amount,
        Integer num_of_splits,
        String merchant,
        String description,
        List<String> categories_set,
        BankAccountResponse bank_account_response
) {
    public TransactionResponse(Transactions transaction) {
        this(
                transaction.getId(),
                transaction.getTransactionDate(),
                transaction.getTotalAmount(),
                transaction.getSplits().size(),
                transaction.getMerchant(),
                transaction.getDescription(),
                transaction.getCategoryNames(),
                new BankAccountResponse(transaction.getBankAccount())
        );
    }
}
