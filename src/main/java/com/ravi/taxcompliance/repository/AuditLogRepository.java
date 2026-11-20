package com.ravi.taxcompliance.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ravi.taxcompliance.entity.AuditLog;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long> {
}