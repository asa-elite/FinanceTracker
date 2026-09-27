package com.example.financetracker.repository;

import com.example.financetracker.model.entity.BankAccount;
import com.example.financetracker.model.entity.TransactionSplit;
import com.example.financetracker.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionSplitRepository extends JpaRepository<TransactionSplit, Long> {
    List<TransactionSplit> getTransactionSplitsByIdAndTransactionBankAccount(Long id, BankAccount transactionBankAccount);

    Optional<TransactionSplit> getTransactionSplitsByIdAndTransactionBankAccountUser(Long aLong, User user);
}
