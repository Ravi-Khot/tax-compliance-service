
package com.ravi.taxcompliance.dto;

import java.math.BigDecimal;
import java.util.Map;

import com.ravi.taxcompliance.enums.ComplianceStatus;

public record CustomerTaxReport(

        String customerId,

        long totalTransactions,

        long successfulTransactions,

        long failedTransactions,

        BigDecimal totalAmount,

        BigDecimal totalExpectedTax,

        BigDecimal totalReportedTax,

        BigDecimal totalTaxGap,

        Map<ComplianceStatus, Long> complianceBreakdown,

        long totalExceptions,

        BigDecimal complianceScore

) {}
