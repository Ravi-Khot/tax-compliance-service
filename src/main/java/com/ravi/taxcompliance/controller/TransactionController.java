package com.ravi.taxcompliance.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ravi.taxcompliance.entity.Transaction;
import com.ravi.taxcompliance.service.TransactionService;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @PostMapping("/upload")
    public ResponseEntity<List<Transaction>> upload(
            @RequestBody List<Map<String, Object>> batch) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.upload(batch));
    }

    @GetMapping
    public List<Transaction> getAll() {
        return service.getAll();
    }
}