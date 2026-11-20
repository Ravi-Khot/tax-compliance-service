
package com.ravi.taxcompliance.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.ravi.taxcompliance.entity.TaxException;
import com.ravi.taxcompliance.entity.TaxRule;
import com.ravi.taxcompliance.entity.Transaction;
import com.ravi.taxcompliance.enums.Severity;
import com.ravi.taxcompliance.enums.AuditEventType;
import com.ravi.taxcompliance.repository.ExceptionRepository;
import com.ravi.taxcompliance.repository.TransactionRepository;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class RuleEngineService {

    private final RuleService ruleService;
    private final TransactionRepository transactionRepository;
    private final ExceptionRepository exceptionRepository;
    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    public RuleEngineService(
            RuleService ruleService,
            TransactionRepository transactionRepository,
            ExceptionRepository exceptionRepository,
            ObjectMapper objectMapper,
            AuditService auditService) {

        this.ruleService = ruleService;
        this.transactionRepository = transactionRepository;
        this.exceptionRepository = exceptionRepository;
        this.objectMapper = objectMapper;
        this.auditService = auditService;
    }

    public void evaluate(Transaction transaction) {

        for (TaxRule rule : ruleService.getActiveRules()) {

            String result;

            try {

                JsonNode config =
                        objectMapper.readTree(rule.getConfigJson());

                boolean exceptionGenerated = false;
                boolean supported = true;

                switch (rule.getRuleName()) {

                    case "HIGH_VALUE_TRANSACTION":
                        exceptionGenerated =
                                checkHighValue(transaction, config);
                        break;

                    case "REFUND_VALIDATION":
                        exceptionGenerated =
                                checkRefund(transaction);
                        break;

                    case "GST_SLAB_VIOLATION":
                        exceptionGenerated =
                                checkGstSlab(transaction, config);
                        break;

                    default:
                        supported = false;
                        break;
                }

                if (!supported) {
                    result = "SKIPPED - Unsupported rule";
                } else if (exceptionGenerated) {
                    result = "EXCEPTION_GENERATED";
                } else {
                    result = "PASSED";
                }

            } catch (Exception e) {

                String details =
                        "Rule: " + rule.getRuleName()
                        + ", Result: FAILED"
                        + ", Reason: " + e.getMessage();

                auditService.log(
                        AuditEventType.RULE_EXECUTION,
                        transaction.getTransactionId(),
                        details
                );

                throw new IllegalStateException(
                        "Rule execution failed: "
                                + rule.getRuleName(), e);
            }

            // Record every rule execution
            String details =
                    "Rule: " + rule.getRuleName()
                    + ", Result: " + result;

            auditService.log(
                    AuditEventType.RULE_EXECUTION,
                    transaction.getTransactionId(),
                    details
            );
        }
    }

    // Rule 1: High value transaction

    private boolean checkHighValue(
            Transaction transaction, JsonNode config) {

        BigDecimal threshold =
                getDecimal(config, "threshold");

        if (transaction.getAmount().compareTo(threshold) > 0) {

            saveException(
                    transaction,
                    "HIGH_VALUE_TRANSACTION",
                    Severity.MEDIUM,
                    "High value transaction detected. Amount: "
                            + transaction.getAmount()
                            + ", Threshold: " + threshold
            );

            return true;
        }

        return false;
    }

    // Rule 2: Refund validation

    private boolean checkRefund(Transaction transaction) {

        if (transaction.getTransactionType() == null ||
                !transaction.getTransactionType()
                        .name().equals("REFUND")) {
            return false;
        }

        String originalId =
                transaction.getOriginalTransactionId();

        if (originalId == null || originalId.isBlank()) {

            saveException(
                    transaction,
                    "REFUND_VALIDATION",
                    Severity.HIGH,
                    "Original transaction reference is missing"
            );

            return true;
        }

        Transaction original =
                transactionRepository
                        .findByTransactionId(originalId)
                        .orElse(null);

        if (original == null) {

            saveException(
                    transaction,
                    "REFUND_VALIDATION",
                    Severity.HIGH,
                    "Original sale transaction not found: "
                            + originalId
            );

            return true;
        }

        if (transaction.getAmount()
                .compareTo(original.getAmount()) > 0) {

            saveException(
                    transaction,
                    "REFUND_VALIDATION",
                    Severity.HIGH,
                    "Refund amount exceeds original sale amount. "
                            + "Refund: " + transaction.getAmount()
                            + ", Original sale: "
                            + original.getAmount()
            );

            return true;
        }

        return false;
    }

    // Rule 3: GST slab validation

    private boolean checkGstSlab(
            Transaction transaction, JsonNode config) {

        JsonNode slabs = config.path("slabs");

        BigDecimal applicableMinimumRate = null;
        BigDecimal applicableThreshold = null;

        for (JsonNode slab : slabs) {

            BigDecimal threshold =
                    getDecimal(slab, "amountThreshold");

            BigDecimal minimumRate =
                    getDecimal(slab, "minimumTaxRate");

            if (transaction.getAmount()
                    .compareTo(threshold) >= 0) {

                if (applicableThreshold == null ||
                        threshold.compareTo(applicableThreshold) > 0) {

                    applicableThreshold = threshold;
                    applicableMinimumRate = minimumRate;
                }
            }
        }

        if (applicableMinimumRate != null &&
                transaction.getTaxRate()
                        .compareTo(applicableMinimumRate) < 0) {

            saveException(
                    transaction,
                    "GST_SLAB_VIOLATION",
                    Severity.HIGH,
                    "GST rate is below the minimum applicable rate. "
                            + "Amount: " + transaction.getAmount()
                            + ", Actual rate: "
                            + transaction.getTaxRate()
                            + ", Minimum rate: "
                            + applicableMinimumRate
            );

            return true;
        }

        return false;
    }

    // Read numeric values from JSON configuration

    private BigDecimal getDecimal(
            JsonNode config, String field) {

        JsonNode value = config.get(field);

        if (value == null || !value.isNumber()) {

            throw new IllegalArgumentException(
                    "Missing or invalid configuration field: "
                            + field);
        }

        return value.decimalValue();
    }

    // Save detected compliance exception

    private void saveException(
            Transaction transaction,
            String ruleName,
            Severity severity,
            String message) {

        TaxException exception = new TaxException();

        exception.setTransactionId(
                transaction.getTransactionId());

        exception.setCustomerId(
                transaction.getCustomerId());

        exception.setRuleName(ruleName);
        exception.setSeverity(severity);
        exception.setMessage(message);

        exceptionRepository.save(exception);
    }
}
