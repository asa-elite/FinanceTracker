package com.example.financetracker.model.entity;

import com.example.financetracker.model.BaseEntity;
import com.example.financetracker.model.types.TransactionLinkType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class TransactionLink extends BaseEntity {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    private Transactions sourceTransaction;

    @ManyToOne
    private Transactions linkedTransaction;

    @Enumerated(EnumType.STRING)
    private TransactionLinkType linkType;
}
