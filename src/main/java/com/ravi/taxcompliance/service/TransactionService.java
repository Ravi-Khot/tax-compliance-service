
package com.ravi.taxcompliance.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.ravi.taxcompliance.entity.Transaction;
import com.ravi.taxcompliance.entity.TaxException;

import com.ravi.taxcompliance.enums.AuditEventType;
import com.ravi.taxcompliance.enums.ComplianceStatus;
import com.ravi.taxcompliance.enums.Severity;
import com.ravi.taxcompliance.enums.TransactionType;
import com.ravi.taxcompliance.enums.ValidationStatus;

import com.ravi.taxcompliance.repository.TransactionRepository;
import com.ravi.taxcompliance.repository.ExceptionRepository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class TransactionService {

    private final TransactionRepository repository;
    private final ExceptionRepository exceptionRepository;
    private final TaxCalculationService taxService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final RuleEngineService ruleEngineService;

    public TransactionService(
            TransactionRepository repository,
            ExceptionRepository exceptionRepository,
            TaxCalculationService taxService,
            AuditService auditService,
            ObjectMapper objectMapper,
            RuleEngineService ruleEngineService) {

        this.repository = repository;
        this.exceptionRepository = exceptionRepository;
        this.taxService = taxService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
        this.ruleEngineService = ruleEngineService;
    }

    // Upload and validate transactions
    @Transactional
    public List<Transaction> upload(
            List<Map<String, Object>> batch) {

        List<Transaction> results = new ArrayList<>();

        for (Map<String, Object> data : batch) {

            Transaction t = new Transaction();
            List<String> errors = new ArrayList<>();

            // Store original transaction as JSON
            try {
                t.setRawTransaction(
                        objectMapper.writeValueAsString(data));

            } catch (JacksonException e) {

                throw new IllegalArgumentException(
                        "Unable to store raw transaction", e);
            }

            String transactionId = value(data, "transactionId");
            String customerId = value(data, "customerId");

            t.setTransactionId(transactionId);
            t.setCustomerId(customerId);

            // Required field validation
            if (blank(transactionId)) {
                errors.add("transactionId is required");
            }

            if (blank(customerId)) {
                errors.add("customerId is required");
            }

            // Date validation
            try {
                t.setDate(LocalDate.parse(value(data, "date")));

            } catch (Exception e) {
                errors.add("Invalid or missing date");
            }

            // Amount validation
            try {
                BigDecimal amount =
                        new BigDecimal(value(data, "amount"));

                if (amount.signum() <= 0) {
                    errors.add("Amount must be greater than zero");
                }

                t.setAmount(amount);

            } catch (Exception e) {
                errors.add("Invalid or missing amount");
            }

            // Tax rate validation
            try {
                BigDecimal rate =
                        new BigDecimal(value(data, "taxRate"));

                if (rate.signum() < 0) {
                    errors.add("Tax rate cannot be negative");
                }

                t.setTaxRate(rate);

            } catch (Exception e) {
                errors.add("Invalid or missing tax rate");
            }

            // Reported tax validation
            if (data.get("reportedTax") != null) {

                try {
                    BigDecimal reported =
                            new BigDecimal(value(data, "reportedTax"));

                    if (reported.signum() < 0) {
                        errors.add("Reported tax cannot be negative");
                    }

                    t.setReportedTax(reported);

                } catch (Exception e) {
                    errors.add("Invalid reported tax");
                }
            }

            // Transaction type validation
            try {
                t.setTransactionType(
                        TransactionType.valueOf(
                                value(data, "transactionType")
                                        .toUpperCase()
                        )
                );

            } catch (Exception e) {
                errors.add("Invalid or missing transaction type");
            }

            // Refund reference
            t.setOriginalTransactionId(
                    value(data, "originalTransactionId"));

            // Duplicate transaction validation
            if (!blank(transactionId)
                    && repository.existsByTransactionId(transactionId)) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Duplicate transaction ID: " + transactionId
                );
            }

            // Validation result and tax calculation
            if (errors.isEmpty()) {

                t.setValidationStatus(ValidationStatus.SUCCESS);

                taxService.calculate(t);

            } else {

                t.setValidationStatus(ValidationStatus.FAILURE);

                t.setFailureReasons(String.join(", ", errors));
            }

            // Save transaction
            Transaction saved = repository.save(t);

            // Generate tax compliance exceptions
            if (saved.getValidationStatus() == ValidationStatus.SUCCESS) {

                createTaxException(saved);

                // Execute dynamic database-configured rules
                ruleEngineService.evaluate(saved);
            }

            // Audit transaction ingestion
            auditService.log(
                    AuditEventType.INGESTION,
                    saved.getTransactionId(),
                    "Validation status: " + saved.getValidationStatus()
                            + ", Reasons: " + saved.getFailureReasons()
            );

            results.add(saved);
        }

        return results;
    }

    // Generate and save tax compliance exceptions
    private void createTaxException(Transaction transaction) {

        ComplianceStatus status =
                transaction.getComplianceStatus();

        if (status == null ||
                status == ComplianceStatus.COMPLIANT) {
            return;
        }

        TaxException exception = new TaxException();

        exception.setTransactionId(
                transaction.getTransactionId());

        exception.setCustomerId(
                transaction.getCustomerId());

        switch (status) {

            case UNDERPAID:

                exception.setRuleName("TAX_UNDERPAYMENT");
                exception.setSeverity(Severity.HIGH);

                exception.setMessage(
                        "Tax underpaid. Expected tax: "
                                + transaction.getExpectedTax()
                                + ", Reported tax: "
                                + transaction.getReportedTax()
                                + ", Tax gap: "
                                + transaction.getTaxGap()
                );

                break;

            case OVERPAID:

                exception.setRuleName("TAX_OVERPAYMENT");
                exception.setSeverity(Severity.MEDIUM);

                exception.setMessage(
                        "Tax overpaid. Expected tax: "
                                + transaction.getExpectedTax()
                                + ", Reported tax: "
                                + transaction.getReportedTax()
                                + ", Tax gap: "
                                + transaction.getTaxGap()
                );

                break;

            case NON_COMPLIANT:

                exception.setRuleName("MISSING_REPORTED_TAX");
                exception.setSeverity(Severity.HIGH);

                exception.setMessage(
                        "Reported tax is missing. Expected tax: "
                                + transaction.getExpectedTax()
                );

                break;

            default:
                return;
        }

        exceptionRepository.save(exception);
    }

    // Retrieve all transactions
    public List<Transaction> getAll() {

        return repository.findAll();
    }

    // Convert input values to String
    private String value(
            Map<String, Object> data, String key) {

        Object v = data.get(key);

        return v == null ? null : v.toString();
    }

    // Check null or empty values
    private boolean blank(String value) {

        return value == null || value.isBlank();
    }
}
