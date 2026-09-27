package com.example.financetracker.repository;

import com.example.financetracker.model.entity.ReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReceiptItemRepository extends JpaRepository<ReceiptItem, Long> {
}
