package com.example.financetracker.controller;

import com.example.financetracker.dto.request.CreateBankAccountRequest;
import com.example.financetracker.dto.request.CreateBankRequest;
import com.example.financetracker.dto.response.AllBankAccountResponse;
import com.example.financetracker.dto.response.BankAccountResponse;
import com.example.financetracker.dto.response.BankDeletePreviewResponse;
import com.example.financetracker.dto.response.BankResponse;
import com.example.financetracker.model.entity.Bank;
import com.example.financetracker.model.types.BankName;
import com.example.financetracker.service.BankingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/bank")
public class BankController {
    private final BankingService bankingService;

    public BankController(BankingService bankingService) {
        this.bankingService = bankingService;
    }

    @PostMapping("/create/bank")
    public ResponseEntity<?> createBank(@RequestBody CreateBankRequest request, Authentication authentication) {
        String email = authentication.getName();
        try {
            Bank bank = this.bankingService.createBank(request, email);
            return ResponseEntity.status(HttpStatus.CREATED).body(bank);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of(
                            "message", e.getMessage()
                    )
            );
        }
    }

    @PostMapping("/create/account")
    public ResponseEntity<?> createBankAccount(@RequestBody CreateBankAccountRequest request, Authentication authentication) {
        String email = authentication.getName();

        try {
            BankAccountResponse account = this.bankingService.createBankAccount(request, email);
            return ResponseEntity.ok(account);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of(
                            "message", e.getMessage()
                    )
            );
        }
    }

    @GetMapping("/{bankName}")
    public ResponseEntity<BankResponse> getBank(@PathVariable String bankName, Authentication authentication) {
        BankName resolvedBankName = BankName.fromString(bankName);
        String email = authentication.getName();
        return ResponseEntity.ok(this.bankingService.getBank(resolvedBankName, email));
    }

    @GetMapping("/list/banks")
    public ResponseEntity<List<BankResponse>> listBanks(Authentication authentication) {
        String email = authentication.getName();
        List<BankResponse> banks = this.bankingService.listBanks(email);
        return ResponseEntity.ok(banks);
    }

    @GetMapping("/banks/accounts")
    public ResponseEntity<List<AllBankAccountResponse>> getBanksWithAccounts(Authentication authentication) {
        String email = authentication.getName();

        return ResponseEntity.ok(
                this.bankingService.getUserBanksWithAccounts(email)
        );
    }

    @GetMapping("/{bankId}/delete-preview")
    public ResponseEntity<BankDeletePreviewResponse> previewDeleteBank(
            Authentication authentication,
            @PathVariable Long bankId
    ) {
        String email = authentication.getName();
        return ResponseEntity.ok(this.bankingService.getDeletePreview(email, bankId));
    }

    @DeleteMapping("/{bankId}")
    public ResponseEntity<Void> deleteBank(
            Authentication authentication,
            @PathVariable Long bankId,
            @RequestParam(defaultValue = "false") boolean confirmed
    ) {
        if (!confirmed) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Delete must be confirmed first. Call /delete-preview before deleting."
            );
        }
        String email = authentication.getName();
        this.bankingService.deleteBank(email, bankId);
        return ResponseEntity.noContent().build();
    }
}
