package com.example.financetracker.repository;

import com.example.financetracker.model.entity.TransactionLink;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionLinkRepository extends JpaRepository<TransactionLink, Long> {
}
