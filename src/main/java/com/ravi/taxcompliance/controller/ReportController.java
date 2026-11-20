
package com.ravi.taxcompliance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ravi.taxcompliance.dto.CustomerTaxReport;
import com.ravi.taxcompliance.dto.ExceptionReport;
import com.ravi.taxcompliance.service.ReportService;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    // Customer-wise tax report
    @GetMapping("/customers/{customerId}")
    public ResponseEntity<CustomerTaxReport> getCustomerReport(
            @PathVariable String customerId) {

        return ResponseEntity.ok(
                reportService.getCustomerReport(customerId)
        );
    }

    // Overall or customer-wise exception report
    @GetMapping("/exceptions")
    public ResponseEntity<ExceptionReport> getExceptionReport(
            @RequestParam(required = false) String customerId) {

        return ResponseEntity.ok(
                reportService.getExceptionReport(customerId)
        );
    }
}
