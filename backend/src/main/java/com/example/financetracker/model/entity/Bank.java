package com.example.financetracker.model.entity;

import com.example.financetracker.model.types.BankName;
import com.example.financetracker.model.BaseEntity;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Table(name = "banks")
public class Bank extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    private BankName name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "bank", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BankAccount> accounts = new ArrayList<>();

    public Bank() {}
}
