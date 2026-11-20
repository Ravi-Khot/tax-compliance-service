
package com.ravi.taxcompliance.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.ravi.taxcompliance.entity.TaxException;
import com.ravi.taxcompliance.enums.Severity;
import com.ravi.taxcompliance.repository.ExceptionRepository;

@Service
public class ExceptionService {

    private final ExceptionRepository exceptionRepository;

    public ExceptionService(ExceptionRepository exceptionRepository) {
        this.exceptionRepository = exceptionRepository;
    }

    // Get all exceptions with optional filters
    public List<TaxException> getExceptions(
            String customerId,
            Severity severity,
            String ruleName) {

        return exceptionRepository.findByFilters(
                customerId,
                severity,
                ruleName
        );
    }

    // Get exception by ID
    public TaxException getExceptionById(Long id) {

        return exceptionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Exception not found with ID: " + id
                ));
    }
}
