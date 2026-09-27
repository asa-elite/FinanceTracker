package com.example.financetracker.service;

import com.example.financetracker.dto.request.CreateBankAccountRequest;
import com.example.financetracker.dto.response.AllBankAccountResponse;
import com.example.financetracker.dto.response.BankAccountResponse;
import com.example.financetracker.dto.response.BankDeletePreviewResponse;
import com.example.financetracker.dto.response.BankResponse;
import com.example.financetracker.model.entity.*;
import com.example.financetracker.model.types.AccountType;
import com.example.financetracker.model.types.BankName;
import com.example.financetracker.repository.BankAccountRepository;
import com.example.financetracker.repository.BankRepository;
import com.example.financetracker.dto.request.CreateBankRequest;
import com.example.financetracker.repository.UserRepository;
import jakarta.transaction.Transaction;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BankingService {
    private final BankRepository bankRepository;
    private final UserRepository userRepository;
    private final BankAccountRepository bankAccountRepository;

    public BankingService(BankRepository bankRepository,
                          UserRepository userRepository,
                          BankAccountRepository bankAccountRepository) {
        this.bankRepository = bankRepository;
        this.userRepository = userRepository;
        this.bankAccountRepository = bankAccountRepository;
    }

    private User getUser(String email) {
        return this.userRepository.findByUsername(email).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public BankResponse getBank(@NotNull long bankId, String userEmail) {
        User user = getUser(userEmail);
        return new BankResponse(this.bankRepository.findBankByIdAndUser(bankId, user));
    }

    public BankResponse getBank(@NotNull BankName bankName, String userEmail) {
        User user = getUser(userEmail);
        return new BankResponse(this.bankRepository.findBankByUserAndName(user, bankName));
    }

    public Bank createBank(@NotNull CreateBankRequest request, @NotNull String email) {
        User user = getUser(email);

        if (this.bankRepository.existsByName(request.name())) {
            throw new RuntimeException("Bank already exists for this user");
        }

        Bank bank = new Bank();
        bank.setName(request.name());
        bank.setUser(user);

        return this.bankRepository.save(bank);
    }

    public BankAccountResponse createBankAccount(CreateBankAccountRequest request, String email) {
        User user = getUser(email);
        Bank bank = this.bankRepository.findBankByIdAndUser(request.bankId(), user);
        AccountType type = AccountType.fromString(request.type());

        if (this.bankAccountRepository.existsByAccountNameAndAccountTypeAndBank(request.name(), type, bank)){
            throw new RuntimeException("Bank account type and name exists for this bank already.");
        }

        BankAccount account = new BankAccount();
        account.setBank(bank);
        account.setUser(user);
        account.setAccountName(request.name());
        account.setAccountType(type);
        account.setBalance(BigDecimal.ZERO);

        BankAccount newAccount = this.bankAccountRepository.save(account);
        return new BankAccountResponse(newAccount);
    }

    public List<BankResponse> listBanks(@NotNull String email) {
        User user = getUser(email);

        return this.bankRepository.findByUser(user)
                .stream()
                .map(bank -> new BankResponse(
                        bank.getId(),
                        bank.getName().toString()
                ))
                .toList();
    }

    public List<AllBankAccountResponse> getUserBanksWithAccounts(@NotNull String email) {
        List<BankAccount> accounts = this.bankAccountRepository.findByUserUsername(email);

        return accounts.stream()
                .collect(Collectors.groupingBy(BankAccount::getBank))
                .entrySet()
                .stream()
                .map(entry -> {
                    Bank bank = entry.getKey();

                    List<BankAccountResponse> accountResponses = entry.getValue()
                            .stream()
                            .map(BankAccountResponse::new)
                            .toList();
                    return new AllBankAccountResponse(
                            bank.getId(),
                            bank.getName().toString(),
                            accountResponses
                    );
                })
                .toList();
    }

    public BankDeletePreviewResponse getDeletePreview(@NotNull String userEmail, @NotNull Long bankId) {
        Bank bank =  this.bankRepository.findBankByIdAndUser(bankId, getUser(userEmail));
        int numAccounts = 0;
        int numTransactions = 0;
        int numSplits = 0;
        int numShares = 0;

        List<BankAccount> accounts = bank.getAccounts();
        numAccounts = accounts.size();
        for  (BankAccount account : accounts) {
            List<Transactions> transactions = account.getTransactions();
            numTransactions = numTransactions + transactions.size();

            for (Transactions transaction : transactions) {
                List<TransactionSplit> splits = transaction.getSplits();
                numSplits = numSplits + splits.size();

                List<TransactionLink> links = transaction.getLinks();
                numShares = numShares + links.size();
            }
        }

        return new BankDeletePreviewResponse(
                bankId,
                bank.getName().toString(),
                numAccounts,
                numTransactions,
                numSplits,
                numShares
        );
    }

    public void deleteBank(@NotNull String userEmail, @NotNull Long bankId) {
        User user = getUser(userEmail);
        Bank bank = this.bankRepository.findBankByIdAndUser(bankId, user);
        this.bankAccountRepository.deleteById(bank.getId());
    }
}
