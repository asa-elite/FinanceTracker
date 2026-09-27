package com.example.financetracker.controller;

import com.example.financetracker.dto.request.*;
import com.example.financetracker.dto.response.StatementImportPreviewResponse;
import com.example.financetracker.dto.response.TransactionResponse;
import com.example.financetracker.dto.response.TransactionShareResponse;
import com.example.financetracker.model.types.TransactionSource;
import com.example.financetracker.service.TransactionService;
import com.example.financetracker.util.parser.PdfOcrService;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/txn")
public class TransactionController {
    private final TransactionService transactionService;
    private final PdfOcrService pdfParser;

    public TransactionController(
            TransactionService transactionService,
            PdfOcrService pdfParser
    ) {
        this.transactionService = transactionService;
        this.pdfParser = pdfParser;
    }

    @PostMapping("/create")
    public ResponseEntity<TransactionResponse> createTransaction(
            @RequestBody CreateTransactionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(this.transactionService.createTransaction(authentication.getName(), request, TransactionSource.USER_INPUT));
    }

    @PostMapping("/create/share")
    public ResponseEntity<TransactionShareResponse> createTransactionShare(
            @RequestBody CreateTransactionShareRequest request,
            Authentication authentication
            ) {
        return ResponseEntity.ok(this.transactionService.createTransactionShare(request, authentication.getName()));
    }

    @GetMapping("/list")
    public ResponseEntity<?> listTransactions(
            Authentication authentication,
            @RequestParam(required = false) LocalDateTime start,
            @RequestParam(required = false) LocalDateTime end,
            @RequestParam(required = false, defaultValue = "2592000") long seconds
    ) {
        try {
            return ResponseEntity.ok(
                    this.transactionService.listTransaction(
                            authentication.getName(),
                            start,
                            end,
                            seconds
                    )
            );
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    Map.of(
                            "message", e.getMessage(),
                            "error", true
                    )
            );
        }
    }

    @PostMapping("/statement-imports/preview/bank-accounts/{bankAccountId}")
    public ResponseEntity<StatementImportPreviewResponse> previewImport(
            Authentication authentication,
            @PathVariable Long bankAccountId,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(
                pdfParser.previewPDFImport(authentication.getName(), bankAccountId, file)
        );
    }

    @PostMapping("/statement-import/confirm")
    public ResponseEntity<Void> confirmStatementImport(
            Authentication authentication,
            @RequestBody ConfirmStatementImportRequest request
    ) {
        transactionService.confirmStatementImport(
                authentication.getName(),
                request
        );

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete")
    public ResponseEntity<TransactionResponse> deleteTransaction(
            Authentication authentication,
            @RequestBody DeleteTransactionRequest request
    ) {
        transactionService.deleteTransaction(
                authentication.getName(),
                request
        );

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete/bulk")
    public ResponseEntity<TransactionResponse> deleteBulkTransaction(
            Authentication authentication,
            @RequestBody DeleteTransactionBulkRequest request
    ) {
        transactionService.deleteTransactionBulk(
                authentication.getName(),
                request
        );
        return ResponseEntity.ok().build();
    }
}
