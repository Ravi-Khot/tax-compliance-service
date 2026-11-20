
package com.ravi.taxcompliance.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ravi.taxcompliance.entity.TaxException;
import com.ravi.taxcompliance.enums.Severity;
import com.ravi.taxcompliance.service.ExceptionService;

@RestController
@RequestMapping("/api/exceptions")
public class ExceptionController {

    private final ExceptionService exceptionService;

    public ExceptionController(ExceptionService exceptionService) {
        this.exceptionService = exceptionService;
    }

    // Get all exceptions or filter exceptions
    @GetMapping
    public ResponseEntity<List<TaxException>> getExceptions(

            @RequestParam(required = false) String customerId,

            @RequestParam(required = false) Severity severity,

            @RequestParam(required = false) String ruleName) {

        List<TaxException> exceptions =
                exceptionService.getExceptions(
                        customerId,
                        severity,
                        ruleName
                );

        return ResponseEntity.ok(exceptions);
    }

    // Get exception by ID
    @GetMapping("/{id}")
    public ResponseEntity<TaxException> getExceptionById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                exceptionService.getExceptionById(id)
        );
    }
}
