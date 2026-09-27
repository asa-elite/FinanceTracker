package com.example.financetracker.model.entity;

import com.example.financetracker.model.types.AccountType;
import com.example.financetracker.model.BaseEntity;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Table(name = "bank_accounts")
public class BankAccount extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_id", nullable = false)
    private Bank bank;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String accountName;

    @Column(nullable = false)
    private AccountType accountType;

    @Column(precision = 19, scale = 2)
    private BigDecimal balance;

    @OneToMany(mappedBy = "bankAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transactions> transactions = new ArrayList<>();

    public BankAccount() {}

    public void applyTransactionAmount(BigDecimal amount) {
        if (this.balance == null) {
            this.balance = BigDecimal.ZERO;
        }

        System.out.println("Transaction amount: $" + amount + " Account total: $" + this.balance);
        this.balance = this.balance.add(amount);

    }
}
