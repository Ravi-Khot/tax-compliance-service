package com.ravi.taxcompliance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ravi.taxcompliance.entity.TaxRule;

public interface TaxRuleRepository
        extends JpaRepository<TaxRule, Long> {

    List<TaxRule> findByEnabledTrue();
}