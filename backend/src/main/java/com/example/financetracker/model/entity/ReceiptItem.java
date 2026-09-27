package com.example.financetracker.model.entity;

import com.example.financetracker.model.BaseEntity;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
public class ReceiptItem extends BaseEntity {

    @Id
    @GeneratedValue
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne
    private Transactions transaction;

    private String itemName;

    private Integer quantity;

    @Column(precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(precision = 19, scale = 2)
    private BigDecimal totalPrice;

    @ManyToOne
    private Category category;
}
