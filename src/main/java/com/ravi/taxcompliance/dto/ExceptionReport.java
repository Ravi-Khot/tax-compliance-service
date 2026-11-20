
package com.ravi.taxcompliance.dto;

import java.util.List;
import java.util.Map;

import com.ravi.taxcompliance.enums.Severity;

public record ExceptionReport(

        long totalExceptions,

        Map<Severity, Long> severityBreakdown,

        Map<String, Long> ruleBreakdown,

        Map<String, Long> customerExceptionCounts,

        List<ExceptionReportItem> exceptions

) {}
