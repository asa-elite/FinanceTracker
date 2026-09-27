package com.example.financetracker.util.parser;

import com.example.financetracker.dto.response.StatementImportPreviewResponse;
import com.example.financetracker.dto.result.StatementParseResult;
import com.example.financetracker.model.entity.Bank;
import com.example.financetracker.model.entity.BankAccount;
import org.springframework.web.multipart.MultipartFile;

public interface StatementParser {
    boolean supports(Bank bank);
    StatementParseResult parse(String text, BankAccount bankAccount);
}
