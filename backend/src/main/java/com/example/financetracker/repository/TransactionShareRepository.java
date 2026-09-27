package com.example.financetracker.repository;

import com.example.financetracker.model.entity.TransactionShare;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionShareRepository extends JpaRepository<TransactionShare, Long> {
}
