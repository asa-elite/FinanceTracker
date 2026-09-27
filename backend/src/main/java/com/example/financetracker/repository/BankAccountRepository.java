package com.example.financetracker.repository;

import com.example.financetracker.model.types.AccountType;
import com.example.financetracker.model.entity.Bank;
import com.example.financetracker.model.entity.BankAccount;
import com.example.financetracker.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    boolean existsByAccountNameAndAccountTypeAndBank(String accountName, AccountType accountType, Bank bank);
    List<BankAccount> findByUserUsername(String userUsername);

    BankAccount getBankAccountByIdAndUser(Long id, User user);
}
