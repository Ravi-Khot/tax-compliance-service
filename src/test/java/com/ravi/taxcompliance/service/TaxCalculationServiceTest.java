
package com.ravi.taxcompliance.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ravi.taxcompliance.entity.Transaction;
import com.ravi.taxcompliance.enums.AuditEventType;
import com.ravi.taxcompliance.enums.ComplianceStatus;

@ExtendWith(MockitoExtension.class)
class TaxCalculationServiceTest {

    @Mock
    private AuditService auditService;

    @InjectMocks
    private TaxCalculationService taxService;

    private Transaction transaction;

    @BeforeEach
    void setUp() {
        transaction = new Transaction();
        transaction.setTransactionId("TXN101");
        transaction.setAmount(new BigDecimal("1000"));
        transaction.setTaxRate(new BigDecimal("0.18"));
    }

    @Test
    void testCompliantTransaction() {
        transaction.setReportedTax(new BigDecimal("180"));

        taxService.calculate(transaction);

        assertEquals(new BigDecimal("180.00"),
                transaction.getExpectedTax());

        assertEquals(new BigDecimal("0.00"),
                transaction.getTaxGap());

        assertEquals(ComplianceStatus.COMPLIANT,
                transaction.getComplianceStatus());
    }

    @Test
    void testUnderpaidTransaction() {
        transaction.setReportedTax(new BigDecimal("150"));

        taxService.calculate(transaction);

        assertEquals(new BigDecimal("30.00"),
                transaction.getTaxGap());

        assertEquals(ComplianceStatus.UNDERPAID,
                transaction.getComplianceStatus());
    }

    @Test
    void testOverpaidTransaction() {
        transaction.setReportedTax(new BigDecimal("200"));

        taxService.calculate(transaction);

        assertEquals(new BigDecimal("-20.00"),
                transaction.getTaxGap());

        assertEquals(ComplianceStatus.OVERPAID,
                transaction.getComplianceStatus());
    }

    @Test
    void testMissingReportedTax() {
        transaction.setReportedTax(null);

        taxService.calculate(transaction);

        assertEquals(new BigDecimal("180.00"),
                transaction.getExpectedTax());

        assertNull(transaction.getTaxGap());

        assertEquals(ComplianceStatus.NON_COMPLIANT,
                transaction.getComplianceStatus());
    }

    @Test
    void testTaxGapWithinOneIsCompliant() {
        transaction.setReportedTax(new BigDecimal("179"));

        taxService.calculate(transaction);

        assertEquals(new BigDecimal("1.00"),
                transaction.getTaxGap());

        assertEquals(ComplianceStatus.COMPLIANT,
                transaction.getComplianceStatus());
    }

    @Test
    void testTaxCalculationRounding() {
        transaction.setAmount(new BigDecimal("1"));
        transaction.setTaxRate(new BigDecimal("0.125"));
        transaction.setReportedTax(new BigDecimal("0.13"));

        taxService.calculate(transaction);

        assertEquals(new BigDecimal("0.13"),
                transaction.getExpectedTax());

        assertEquals(new BigDecimal("0.00"),
                transaction.getTaxGap());
    }

    @Test
    void testAuditLogIsCalled() {
        transaction.setReportedTax(new BigDecimal("180"));

        taxService.calculate(transaction);

        verify(auditService, times(1)).log(
                eq(AuditEventType.TAX_COMPUTATION),
                eq("TXN101"),
                contains("Expected tax: 180.00")
        );
    }
}


//Test	Expected result
//Compliant transaction	COMPLIANT
//Underpaid transaction	UNDERPAID
//Overpaid transaction	OVERPAID
//Missing reported tax	NON_COMPLIANT
//Tax gap exactly 1	COMPLIANT
//Rounding calculation	0.13
//Audit logging	Verify called once
