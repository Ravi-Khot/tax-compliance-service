package com.ravi.taxcompliance.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ravi.taxcompliance.entity.AuditLog;
import com.ravi.taxcompliance.enums.AuditEventType;
import com.ravi.taxcompliance.repository.AuditLogRepository;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository repository;

    @InjectMocks
    private AuditService auditService;

    @Test
    void testAuditLogCreation() {
        auditService.log(
                AuditEventType.INGESTION,
                "TXN101",
                "Transaction uploaded successfully"
        );

        ArgumentCaptor<AuditLog> captor =
                ArgumentCaptor.forClass(AuditLog.class);

        verify(repository).save(captor.capture());

        AuditLog savedLog = captor.getValue();

        assertEquals(AuditEventType.INGESTION,
                savedLog.getEventType());
        assertEquals("TXN101", savedLog.getTransactionId());
        assertEquals("Transaction uploaded successfully",
                savedLog.getDetailJson());
    }
}