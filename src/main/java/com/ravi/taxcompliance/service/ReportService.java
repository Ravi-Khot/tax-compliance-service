
package com.ravi.taxcompliance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.ravi.taxcompliance.dto.CustomerTaxReport;
import com.ravi.taxcompliance.dto.ExceptionReport;
import com.ravi.taxcompliance.dto.ExceptionReportItem;
import com.ravi.taxcompliance.entity.Transaction;
import com.ravi.taxcompliance.entity.TaxException;
import com.ravi.taxcompliance.enums.ComplianceStatus;
import com.ravi.taxcompliance.enums.ValidationStatus;
import com.ravi.taxcompliance.enums.Severity;
import com.ravi.taxcompliance.repository.TransactionRepository;
import com.ravi.taxcompliance.repository.ExceptionRepository;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final TransactionRepository transactionRepository;
    private final ExceptionRepository exceptionRepository;

    public ReportService(
            TransactionRepository transactionRepository,
            ExceptionRepository exceptionRepository) {

        this.transactionRepository = transactionRepository;
        this.exceptionRepository = exceptionRepository;
    }

    // Customer-wise tax report

    public CustomerTaxReport getCustomerReport(String customerId) {

        if (!StringUtils.hasText(customerId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Customer ID is required"
            );
        }

        List<Transaction> transactions =
                transactionRepository.findByCustomerId(customerId);

        long totalTransactions = transactions.size();

        long successfulTransactions = transactions.stream()
                .filter(t -> t.getValidationStatus()
                        == ValidationStatus.SUCCESS)
                .count();

        long failedTransactions = transactions.stream()
                .filter(t -> t.getValidationStatus()
                        == ValidationStatus.FAILURE)
                .count();

        BigDecimal totalAmount = transactions.stream()
                .map(Transaction::getAmount)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpectedTax = transactions.stream()
                .map(Transaction::getExpectedTax)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalReportedTax = transactions.stream()
                .map(Transaction::getReportedTax)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Gross tax discrepancy (absolute gaps)

        BigDecimal totalTaxGap = transactions.stream()
                .map(Transaction::getTaxGap)
                .filter(value -> value != null)
                .map(BigDecimal::abs)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Compliance status breakdown

        Map<ComplianceStatus, Long> complianceBreakdown =
                new EnumMap<>(ComplianceStatus.class);

        for (ComplianceStatus status : ComplianceStatus.values()) {

            long count = transactions.stream()
                    .filter(t -> t.getComplianceStatus() == status)
                    .count();

            complianceBreakdown.put(status, count);
        }

        // Total exceptions for customer

        long totalExceptions =
                exceptionRepository.findByCustomerId(customerId).size();

        // Calculate compliance score

     // Calculate compliance score as per assessment formula
        long nonCompliantTransactions =
                complianceBreakdown.getOrDefault(
                        ComplianceStatus.NON_COMPLIANT, 0L);

        BigDecimal complianceScore = BigDecimal.ZERO;

        if (totalTransactions > 0) {
            complianceScore = BigDecimal.valueOf(100)
                    .subtract(
                            BigDecimal.valueOf(nonCompliantTransactions)
                                    .multiply(BigDecimal.valueOf(100))
                                    .divide(
                                            BigDecimal.valueOf(totalTransactions),
                                            2,
                                            RoundingMode.HALF_UP
                                    )
                    );
        }

        return new CustomerTaxReport(
                customerId,
                totalTransactions,
                successfulTransactions,
                failedTransactions,
                totalAmount,
                totalExpectedTax,
                totalReportedTax,
                totalTaxGap,
                complianceBreakdown,
                totalExceptions,
                complianceScore
        );
    }

    // Overall or customer-wise exception report

    public ExceptionReport getExceptionReport(String customerId) {

        List<TaxException> exceptions;

        if (StringUtils.hasText(customerId)) {

            exceptions =
                    exceptionRepository.findByCustomerId(customerId);

        } else {

            exceptions = exceptionRepository.findAll();
        }

        // Severity breakdown

        Map<Severity, Long> severityBreakdown =
                new EnumMap<>(Severity.class);

        for (Severity severity : Severity.values()) {

            long count = exceptions.stream()
                    .filter(e -> e.getSeverity() == severity)
                    .count();

            severityBreakdown.put(severity, count);
        }

        // Rule-wise exception breakdown

        Map<String, Long> ruleBreakdown = exceptions.stream()
                .collect(Collectors.groupingBy(
                        TaxException::getRuleName,
                        TreeMap::new,
                        Collectors.counting()
                ));

        // Customer-wise exception counts

        Map<String, Long> customerExceptionCounts =
                exceptions.stream()
                        .collect(Collectors.groupingBy(
                                e -> e.getCustomerId() == null
                                        ? "UNKNOWN"
                                        : e.getCustomerId(),
                                TreeMap::new,
                                Collectors.counting()
                        ));

        // Exception details

        List<ExceptionReportItem> items = exceptions.stream()
                .map(e -> new ExceptionReportItem(
                        e.getId(),
                        e.getTransactionId(),
                        e.getCustomerId(),
                        e.getRuleName(),
                        e.getSeverity(),
                        e.getMessage(),
                        e.getTimestamp()
                ))
                .toList();

        return new ExceptionReport(
                exceptions.size(),
                severityBreakdown,
                ruleBreakdown,
                customerExceptionCounts,
                items
        );
    }
}
