package com.example.financetracker.repository;

import com.example.financetracker.model.entity.Bank;
import com.example.financetracker.model.types.BankName;
import com.example.financetracker.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankRepository extends JpaRepository<Bank, Long> {
    boolean existsByName(BankName name);
    List<Bank> findByUser(User user);
    Bank findBankByUserAndName(User user, BankName name);
    Bank findBankByIdAndUser(Long id, User user);
}
