package com.example.financetracker.repository;

import com.example.financetracker.model.entity.Transactions;
import com.example.financetracker.model.entity.User;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transactions, Long> {
    List<Transactions> findByTransactionDateBetween(LocalDateTime transactionDateAfter, LocalDateTime transactionDateBefore);

    List<Transactions> findByTransactionDateBetweenAndBankAccountUser(LocalDateTime start, LocalDateTime end, User bankAccountUser);

    Optional<Transactions> findByBankAccountUserAndId(User bankAccountUser, Long id);

    List<Transactions> findAllByIdInAndBankAccountUser(@NotNull List<Long> longs, User user);
}
