
package com.ravi.taxcompliance.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.databind.ObjectMapper;

import com.ravi.taxcompliance.entity.TaxException;
import com.ravi.taxcompliance.entity.TaxRule;
import com.ravi.taxcompliance.entity.Transaction;
import com.ravi.taxcompliance.enums.Severity;
import com.ravi.taxcompliance.enums.TransactionType;
import com.ravi.taxcompliance.repository.ExceptionRepository;
import com.ravi.taxcompliance.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class RuleEngineServiceTest {

    @Mock
    private RuleService ruleService;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ExceptionRepository exceptionRepository;

    @Mock
    private AuditService auditService;

    private RuleEngineService ruleEngineService;

    @BeforeEach
    void setUp() {
        ruleEngineService = new RuleEngineService(
                ruleService,
                transactionRepository,
                exceptionRepository,
                new ObjectMapper(),
                auditService
        );

        lenient().when(exceptionRepository.save(any(TaxException.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Transaction createTransaction() {
        Transaction transaction = new Transaction();
        transaction.setTransactionId("TXN101");
        transaction.setCustomerId("CUS101");
        transaction.setAmount(new BigDecimal("2000"));
        transaction.setTaxRate(new BigDecimal("0.18"));
        transaction.setTransactionType(TransactionType.SALE);
        return transaction;
    }

    private TaxRule createRule(String name, String config) {
        TaxRule rule = new TaxRule();
        rule.setRuleName(name);
        rule.setConfigJson(config);
        rule.setEnabled(true);
        return rule;
    }

    @Test
    void testHighValueTransaction() {
        TaxRule rule = createRule(
                "HIGH_VALUE_TRANSACTION",
                "{\"threshold\":1000}"
        );

        when(ruleService.getActiveRules()).thenReturn(List.of(rule));

        ruleEngineService.evaluate(createTransaction());

        ArgumentCaptor<TaxException> captor =
                ArgumentCaptor.forClass(TaxException.class);

        verify(exceptionRepository).save(captor.capture());

        assertEquals("HIGH_VALUE_TRANSACTION",
                captor.getValue().getRuleName());
        assertEquals(Severity.MEDIUM,
                captor.getValue().getSeverity());
    }

    @Test
    void testHighValueRuleNotTriggered() {
        TaxRule rule = createRule(
                "HIGH_VALUE_TRANSACTION",
                "{\"threshold\":3000}"
        );

        when(ruleService.getActiveRules()).thenReturn(List.of(rule));

        ruleEngineService.evaluate(createTransaction());

        verify(exceptionRepository, never()).save(any());
    }

    @Test
    void testRefundMissingOriginalTransactionId() {
        TaxRule rule = createRule("REFUND_VALIDATION", "{}");

        Transaction transaction = createTransaction();
        transaction.setTransactionType(TransactionType.REFUND);
        transaction.setOriginalTransactionId(null);

        when(ruleService.getActiveRules()).thenReturn(List.of(rule));

        ruleEngineService.evaluate(transaction);

        verify(exceptionRepository).save(any(TaxException.class));
    }

    @Test
    void testRefundOriginalTransactionNotFound() {
        TaxRule rule = createRule("REFUND_VALIDATION", "{}");

        Transaction transaction = createTransaction();
        transaction.setTransactionType(TransactionType.REFUND);
        transaction.setOriginalTransactionId("TXN-ORIGINAL");

        when(ruleService.getActiveRules()).thenReturn(List.of(rule));
        when(transactionRepository.findByTransactionId("TXN-ORIGINAL"))
                .thenReturn(Optional.empty());

        ruleEngineService.evaluate(transaction);

        verify(exceptionRepository).save(any(TaxException.class));
    }

    @Test
    void testRefundExceedsOriginalAmount() {
        TaxRule rule = createRule("REFUND_VALIDATION", "{}");

        Transaction transaction = createTransaction();
        transaction.setTransactionType(TransactionType.REFUND);
        transaction.setOriginalTransactionId("TXN-ORIGINAL");
        transaction.setAmount(new BigDecimal("1500"));

        Transaction original = new Transaction();
        original.setAmount(new BigDecimal("1000"));

        when(ruleService.getActiveRules()).thenReturn(List.of(rule));
        when(transactionRepository.findByTransactionId("TXN-ORIGINAL"))
                .thenReturn(Optional.of(original));

        ruleEngineService.evaluate(transaction);

        verify(exceptionRepository).save(any(TaxException.class));
    }

    @Test
    void testValidRefund() {
        TaxRule rule = createRule("REFUND_VALIDATION", "{}");

        Transaction transaction = createTransaction();
        transaction.setTransactionType(TransactionType.REFUND);
        transaction.setOriginalTransactionId("TXN-ORIGINAL");
        transaction.setAmount(new BigDecimal("500"));

        Transaction original = new Transaction();
        original.setAmount(new BigDecimal("1000"));

        when(ruleService.getActiveRules()).thenReturn(List.of(rule));
        when(transactionRepository.findByTransactionId("TXN-ORIGINAL"))
                .thenReturn(Optional.of(original));

        ruleEngineService.evaluate(transaction);

        verify(exceptionRepository, never()).save(any());
    }

    @Test
    void testGstSlabViolation() {
        TaxRule rule = createRule(
                "GST_SLAB_VIOLATION",
                "{\"slabs\":[{\"amountThreshold\":1000,"
                + "\"minimumTaxRate\":0.18}]}"
        );

        Transaction transaction = createTransaction();
        transaction.setTaxRate(new BigDecimal("0.05"));

        when(ruleService.getActiveRules()).thenReturn(List.of(rule));

        ruleEngineService.evaluate(transaction);

        ArgumentCaptor<TaxException> captor =
                ArgumentCaptor.forClass(TaxException.class);

        verify(exceptionRepository).save(captor.capture());

        assertEquals("GST_SLAB_VIOLATION",
                captor.getValue().getRuleName());
        assertEquals(Severity.HIGH,
                captor.getValue().getSeverity());
    }

    @Test
    void testGstSlabRulePasses() {
        TaxRule rule = createRule(
                "GST_SLAB_VIOLATION",
                "{\"slabs\":[{\"amountThreshold\":1000,"
                + "\"minimumTaxRate\":0.18}]}"
        );

        Transaction transaction = createTransaction();
        transaction.setTaxRate(new BigDecimal("0.18"));

        when(ruleService.getActiveRules()).thenReturn(List.of(rule));

        ruleEngineService.evaluate(transaction);

        verify(exceptionRepository, never()).save(any());
    }

    @Test
    void testUnsupportedRule() {
        TaxRule rule = createRule("UNKNOWN_RULE", "{}");

        when(ruleService.getActiveRules()).thenReturn(List.of(rule));

        assertDoesNotThrow(
                () -> ruleEngineService.evaluate(createTransaction())
        );

        verify(exceptionRepository, never()).save(any());
        verify(auditService).log(any(), any(), contains("SKIPPED"));
    }

    @Test
    void testInvalidRuleConfiguration() {
        TaxRule rule = createRule(
                "HIGH_VALUE_TRANSACTION",
                "invalid-json"
        );

        when(ruleService.getActiveRules()).thenReturn(List.of(rule));

        assertThrows(IllegalStateException.class,
                () -> ruleEngineService.evaluate(createTransaction()));

        verify(auditService).log(any(), any(), contains("FAILED"));
    }
}
