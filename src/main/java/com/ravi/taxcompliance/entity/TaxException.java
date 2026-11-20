package com.ravi.taxcompliance.entity;

import java.time.LocalDateTime;

import com.ravi.taxcompliance.enums.Severity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "tax_exceptions")
@Getter
@Setter
public class TaxException {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String transactionId;

    private String customerId;

    private String ruleName;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    private String message;

    private LocalDateTime timestamp = LocalDateTime.now();
}