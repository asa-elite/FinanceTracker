package com.example.financetracker.service;

import com.example.financetracker.dto.request.*;
import com.example.financetracker.dto.response.TransactionResponse;
import com.example.financetracker.dto.response.TransactionShareResponse;
import com.example.financetracker.model.entity.*;
import com.example.financetracker.model.types.ShareStatus;
import com.example.financetracker.model.types.TransactionSource;
import com.example.financetracker.repository.*;
import org.apache.coyote.BadRequestException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final TransactionSplitRepository transactionSplitRepository;
    private final TransactionLinkRepository transactionLinkRepository;
    private final TransactionShareRepository transactionShareRepository;
    private final ReceiptItemRepository receiptItemRepository;
    private final CategoryRepository categoryRepository;
    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            TransactionSplitRepository transactionSplitRepository,
            TransactionLinkRepository transactionLinkRepository,
            TransactionShareRepository transactionShareRepository,
            ReceiptItemRepository receiptItemRepository,
            CategoryRepository categoryRepository,
            BankAccountRepository bankAccountRepository,
            UserRepository userRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.transactionSplitRepository = transactionSplitRepository;
        this.transactionLinkRepository = transactionLinkRepository;
        this.transactionShareRepository = transactionShareRepository;
        this.receiptItemRepository = receiptItemRepository;
        this.categoryRepository = categoryRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.userRepository = userRepository;
    }

    private User getUser(String email) {
        return this.userRepository.findByUsername(email).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public TransactionResponse createTransaction(
            String email,
            CreateTransactionRequest request,
            TransactionSource source
            ) {
        User user = getUser(email);
        BankAccount account = this.bankAccountRepository.getBankAccountByIdAndUser(request.bankId(), user);

        Transactions transaction = new Transactions();
        transaction.setTransactionDate(request.transactionDate());
        transaction.setDescription(request.description());
        transaction.setTotalAmount(request.totalAmount());
        transaction.setBankAccount(account);
        transaction.setSource(source);

        account.applyTransactionAmount(transaction.getTotalAmount());

        for (CreateTransactionSplitRequest req: request.splits()) {
            transaction.addSplit(createSplit(req));
        }

        account.getTransactions().add(transaction);
        return new TransactionResponse(this.transactionRepository.save(transaction));
    }

    private TransactionSplit createSplit(CreateTransactionSplitRequest request) {
        TransactionSplit split = new TransactionSplit();
        split.setAmount(request.amount());
        split.setCategory(createCategory(request.category()));
        return split;
    }

    private Category createCategory(CreateCategoryRequest request) {
        Category cat = new Category();
        cat.setName(request.name());

        if (request.parent() != null) {
            cat.setParentCategory(createCategory(request.parent()));
        }
        return this.categoryRepository.save(cat);
    }

    public List<TransactionResponse> listTransaction(
            String email,
            LocalDateTime start,
            LocalDateTime end,
            long seconds
    ) throws BadRequestException {
        User user = getUser(email);

        LocalDateTime effectiveStart;
        LocalDateTime effectiveEnd;

        if (end != null && start == null) {
            throw new BadRequestException("start is required when end is provided");
        }

        if (start != null && end != null) {
            effectiveStart = start;
            effectiveEnd = end;
        } else if (start != null) {
            effectiveStart = start;
            effectiveEnd = effectiveStart.plusSeconds(seconds);
        } else {
            effectiveEnd = LocalDateTime.now();
            effectiveStart = effectiveEnd.minusSeconds(seconds);
        }

        if (effectiveEnd.isBefore(effectiveStart)) {
            throw new BadRequestException("end is before start date");
        }

        List<Transactions> transactions = this.transactionRepository.findByTransactionDateBetweenAndBankAccountUser(effectiveStart, effectiveEnd, user);
        List<TransactionResponse> listResponse = new ArrayList<>();

        for (Transactions transaction: transactions) {
            listResponse.add(new TransactionResponse(transaction));
        }

        return listResponse;
    }

    public TransactionShareResponse createTransactionShare(
            CreateTransactionShareRequest request,
            String email
    ) {
        User user = getUser(email);
        TransactionSplit split = this.transactionSplitRepository.getTransactionSplitsByIdAndTransactionBankAccountUser(request.transaction_id(), user)
                .orElseThrow(() -> new RuntimeException("Transaction split not found"));

        TransactionShare share = new TransactionShare();
        share.setCreatedByOwner(true);
        share.setParticipantEmail(request.user_name());
        share.setSplit(split);
        share.setStatus(ShareStatus.PENDING);
        return new TransactionShareResponse(this.transactionShareRepository.save(share));
    }

    public void confirmStatementImport(
            String email,
            ConfirmStatementImportRequest request
    ) {
        User user = getUser(email);
        BankAccount account = bankAccountRepository.getBankAccountByIdAndUser(request.bankAccountId(), user);

        List<Transactions> transactions = request.transactions()
                .stream()
                        .map(
                                dto -> {
                                    Transactions transaction = new Transactions();
                                    transaction.setTransactionDate(dto.transaction_date());
                                    transaction.setMerchant(dto.merchant());
                                    transaction.setDescription(dto.description());
                                    transaction.setTotalAmount(dto.total_amount());
                                    transaction.setBankAccount(account);
                                    transaction.setSource(TransactionSource.PDF_IMPORT);
                                    return transaction;
                                }
                        )
                                .toList();

        for (Transactions transaction: transactions) {
            account.applyTransactionAmount(transaction.getTotalAmount());
        }

        transactionRepository.saveAll(transactions);
    }

    public void deleteTransaction(
            String email,
            DeleteTransactionRequest request
    ) {
        User user = getUser(email);
        Transactions transaction = transactionRepository
                .findByBankAccountUserAndId(user, request.transactionId())
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        BankAccount account = transaction.getBankAccount();

        account.applyTransactionAmount(transaction.getTotalAmount().negate());

        transactionRepository.delete(transaction);

    }

    public void deleteTransactionBulk(
            String email,
            DeleteTransactionBulkRequest request
    ) {
        User user = getUser(email);
        List<Transactions> transactions =
                transactionRepository.findAllByIdInAndBankAccountUser(
                        request.transactionIdList(),
                        user
                );

        for (Transactions transaction : transactions) {
            transaction.getBankAccount()
                    .applyTransactionAmount(transaction.getTotalAmount().negate());
        }

        transactionRepository.deleteAll(transactions);
    }

}
