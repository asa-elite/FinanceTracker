package com.example.financetracker.model.entity;

import com.example.financetracker.model.BaseEntity;
import com.example.financetracker.model.types.TransactionSource;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Table(name = "transactions")
public class Transactions extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    Long id;

    @Column(precision = 19, scale = 2, nullable = false)
    BigDecimal totalAmount;

    @Column(nullable = false)
    LocalDateTime transactionDate;

    private String description;
    private String merchant;

    @Enumerated(EnumType.STRING)
    private TransactionSource source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bank_account_id", nullable = false)
    private BankAccount bankAccount;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TransactionSplit> splits = new ArrayList<>();

    @OneToMany(mappedBy = "sourceTransaction", cascade =  CascadeType.ALL, orphanRemoval = true)
    private List<TransactionLink> links = new ArrayList<>();

    public List<String> getCategoryNames() {
        List<String> categories = new ArrayList<>();
        for (TransactionSplit split: splits) {
            categories.add(split.getCategory().getName());

            if (!split.getCategory().parentIsBlank()) {categories.add(split.getCategory().getParentCategory().getName());}
        }
        return categories;
    }

    public void addSplit(TransactionSplit split) {
        splits.add(split);
        split.setTransaction(this);
    }
}
