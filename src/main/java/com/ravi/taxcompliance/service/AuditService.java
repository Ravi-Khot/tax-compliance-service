package com.ravi.taxcompliance.service;

import org.springframework.stereotype.Service;

import com.ravi.taxcompliance.entity.AuditLog;
import com.ravi.taxcompliance.enums.AuditEventType;
import com.ravi.taxcompliance.repository.AuditLogRepository;

@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    public void log(AuditEventType eventType,
                    String transactionId,
                    String detail) {

        AuditLog log = new AuditLog();

        log.setEventType(eventType);
        log.setTransactionId(transactionId);
        log.setDetailJson(detail);

        repository.save(log);
    }
}