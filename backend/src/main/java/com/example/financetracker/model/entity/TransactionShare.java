package com.example.financetracker.model.entity;

import com.example.financetracker.model.BaseEntity;
import com.example.financetracker.model.types.ShareStatus;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class TransactionShare extends BaseEntity {

    @Id
    @GeneratedValue
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_split_id", nullable = false)
    private TransactionSplit split;

    @Column(nullable = false)
    private String participantEmail;

    private boolean createdByOwner;

    @Enumerated(EnumType.STRING)
    private ShareStatus status;
}
