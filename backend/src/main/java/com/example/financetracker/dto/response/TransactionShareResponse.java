package com.example.financetracker.dto.response;

import com.example.financetracker.model.entity.TransactionShare;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionShareResponse(
        String shared_with,
        BigDecimal amount_shared,
        boolean is_my_debt,
        String merchant,
        LocalDateTime transaction_date
) {
    public TransactionShareResponse(TransactionShare transaction) {
        this(
                transaction.getParticipantEmail(),
                transaction.getSplit().getAmount(),
                !transaction.isCreatedByOwner(),
                transaction.getSplit().getTransaction().getMerchant(),
                transaction.getSplit().getTransaction().getTransactionDate()
        );
    }
}
