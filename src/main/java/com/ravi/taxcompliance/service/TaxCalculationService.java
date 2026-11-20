package com.ravi.taxcompliance.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;

import com.ravi.taxcompliance.entity.Transaction;
import com.ravi.taxcompliance.enums.*;

@Service
public class TaxCalculationService {

    private final AuditService auditService;

    public TaxCalculationService(AuditService auditService) {
        this.auditService = auditService;
    }

    public void calculate(Transaction transaction) {

        BigDecimal expectedTax = transaction.getAmount()
                .multiply(transaction.getTaxRate())
                .setScale(2, RoundingMode.HALF_UP);

        transaction.setExpectedTax(expectedTax);

        if (transaction.getReportedTax() == null) {

            transaction.setTaxGap(null);
            transaction.setComplianceStatus(
                    ComplianceStatus.NON_COMPLIANT);

        } else {

            BigDecimal taxGap = expectedTax
                    .subtract(transaction.getReportedTax())
                    .setScale(2, RoundingMode.HALF_UP);

            transaction.setTaxGap(taxGap);

            if (taxGap.abs().compareTo(BigDecimal.ONE) <= 0) {
                transaction.setComplianceStatus(
                        ComplianceStatus.COMPLIANT);

            } else if (taxGap.compareTo(BigDecimal.ONE) > 0) {
                transaction.setComplianceStatus(
                        ComplianceStatus.UNDERPAID);

            } else {
                transaction.setComplianceStatus(
                        ComplianceStatus.OVERPAID);
            }
        }

        auditService.log(
                AuditEventType.TAX_COMPUTATION,
                transaction.getTransactionId(),
                "Expected tax: " + expectedTax
                + ", Tax gap: " + transaction.getTaxGap()
                + ", Status: " + transaction.getComplianceStatus()
        );
    }
}