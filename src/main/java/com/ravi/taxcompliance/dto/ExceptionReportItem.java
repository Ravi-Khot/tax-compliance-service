
package com.ravi.taxcompliance.dto;

import java.time.LocalDateTime;

import com.ravi.taxcompliance.enums.Severity;

public record ExceptionReportItem(

        Long id,

        String transactionId,

        String customerId,

        String ruleName,

        Severity severity,

        String message,

        LocalDateTime timestamp

) {}
