
package com.ravi.taxcompliance.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ravi.taxcompliance.entity.TaxRule;
import com.ravi.taxcompliance.repository.TaxRuleRepository;

@Service
public class RuleService {

    private final TaxRuleRepository ruleRepository;

    public RuleService(TaxRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    public List<TaxRule> getActiveRules() {
        return ruleRepository.findByEnabledTrue();
    }
}
