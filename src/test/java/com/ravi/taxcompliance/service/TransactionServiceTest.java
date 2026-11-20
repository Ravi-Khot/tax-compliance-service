
package com.ravi.taxcompliance.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.ObjectMapper;

import com.ravi.taxcompliance.entity.Transaction;
import com.ravi.taxcompliance.enums.ValidationStatus;
import com.ravi.taxcompliance.repository.TransactionRepository;
import com.ravi.taxcompliance.repository.ExceptionRepository;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository repository;

    @Mock
    private ExceptionRepository exceptionRepository;

    @Mock
    private TaxCalculationService taxService;

    @Mock
    private AuditService auditService;

    @Mock
    private RuleEngineService ruleEngineService;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(
                repository,
                exceptionRepository,
                taxService,
                auditService,
                new ObjectMapper(),
                ruleEngineService
        );

        lenient().when(repository.existsByTransactionId(any()))
                .thenReturn(false);

        lenient().when(repository.save(any(Transaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Map<String, Object> validTransaction() {
        Map<String, Object> data = new HashMap<>();
        data.put("transactionId", "TXN101");
        data.put("customerId", "CUS101");
        data.put("date", "2026-11-20");
        data.put("amount", "1000");
        data.put("taxRate", "0.18");
        data.put("reportedTax", "180");
        data.put("transactionType", "SALE");
        return data;
    }

    @Test
    void testValidTransactionUpload() {
        List<Transaction> result =
                transactionService.upload(List.of(validTransaction()));

        assertEquals(1, result.size());
        assertEquals(ValidationStatus.SUCCESS,
                result.get(0).getValidationStatus());

        verify(taxService).calculate(any(Transaction.class));
        verify(ruleEngineService).evaluate(any(Transaction.class));
        verify(repository).save(any(Transaction.class));
    }

    @Test
    void testMissingRequiredFields() {
        Map<String, Object> data = new HashMap<>();

        Transaction result =
                transactionService.upload(List.of(data)).get(0);

        assertEquals(ValidationStatus.FAILURE,
                result.getValidationStatus());
        assertNotNull(result.getFailureReasons());

        verify(taxService, never()).calculate(any());
        verify(ruleEngineService, never()).evaluate(any());
    }

    @Test
    void testInvalidDate() {
        Map<String, Object> data = validTransaction();
        data.put("date", "invalid-date");

        Transaction result =
                transactionService.upload(List.of(data)).get(0);

        assertEquals(ValidationStatus.FAILURE,
                result.getValidationStatus());
        assertTrue(result.getFailureReasons()
                .contains("Invalid or missing date"));
    }

    @Test
    void testZeroAmount() {
        Map<String, Object> data = validTransaction();
        data.put("amount", "0");

        Transaction result =
                transactionService.upload(List.of(data)).get(0);

        assertEquals(ValidationStatus.FAILURE,
                result.getValidationStatus());
        assertTrue(result.getFailureReasons()
                .contains("Amount must be greater than zero"));
    }

    @Test
    void testNegativeTaxRate() {
        Map<String, Object> data = validTransaction();
        data.put("taxRate", "-0.18");

        Transaction result =
                transactionService.upload(List.of(data)).get(0);

        assertEquals(ValidationStatus.FAILURE,
                result.getValidationStatus());
        assertTrue(result.getFailureReasons()
                .contains("Tax rate cannot be negative"));
    }

    @Test
    void testInvalidTransactionType() {
        Map<String, Object> data = validTransaction();
        data.put("transactionType", "INVALID");

        Transaction result =
                transactionService.upload(List.of(data)).get(0);

        assertEquals(ValidationStatus.FAILURE,
                result.getValidationStatus());
        assertTrue(result.getFailureReasons()
                .contains("Invalid or missing transaction type"));
    }

    @Test
    void testDuplicateTransactionId() {
        Map<String, Object> data = validTransaction();

        when(repository.existsByTransactionId("TXN101"))
                .thenReturn(true);

        assertThrows(ResponseStatusException.class,
                () -> transactionService.upload(List.of(data)));

        verify(repository, never()).save(any(Transaction.class));
    }

    @Test
    void testMissingReportedTax() {
        Map<String, Object> data = validTransaction();
        data.remove("reportedTax");

        Transaction result =
                transactionService.upload(List.of(data)).get(0);

        assertEquals(ValidationStatus.SUCCESS,
                result.getValidationStatus());

        verify(taxService).calculate(any(Transaction.class));
    }
}

//mvn clean verify
//start target\site\jacoco\index.html