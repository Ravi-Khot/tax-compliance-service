package com.ravi.taxcompliance.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.ravi.taxcompliance.enums.*;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "transactions")
@Getter
@Setter
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String transactionId;

    private LocalDate date;

    private String customerId;

    @Column(precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(precision = 10, scale = 6)
    private BigDecimal taxRate;

    @Column(precision = 19, scale = 4)
    private BigDecimal reportedTax;

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    private ValidationStatus validationStatus;

    @Enumerated(EnumType.STRING)
    private ComplianceStatus complianceStatus;

    @Column(precision = 19, scale = 4)
    private BigDecimal expectedTax;

    @Column(precision = 19, scale = 4)
    private BigDecimal taxGap;

    @Column(columnDefinition = "TEXT")
    private String rawTransaction;

    @Column(columnDefinition = "TEXT")
    private String failureReasons;

    private String originalTransactionId;

    private LocalDateTime createdAt = LocalDateTime.now();
}