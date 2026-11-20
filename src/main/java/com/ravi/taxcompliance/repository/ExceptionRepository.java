
package com.ravi.taxcompliance.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ravi.taxcompliance.entity.TaxException;
import com.ravi.taxcompliance.enums.Severity;

public interface ExceptionRepository
        extends JpaRepository<TaxException, Long> {

    List<TaxException> findByCustomerId(String customerId);

    List<TaxException> findBySeverity(Severity severity);

    List<TaxException> findByRuleName(String ruleName);

    @Query("""
        SELECT e FROM TaxException e
        WHERE (:customerId IS NULL OR e.customerId = :customerId)
        AND (:severity IS NULL OR e.severity = :severity)
        AND (:ruleName IS NULL OR e.ruleName = :ruleName)
        ORDER BY e.timestamp DESC
        """)
    List<TaxException> findByFilters(
            @Param("customerId") String customerId,
            @Param("severity") Severity severity,
            @Param("ruleName") String ruleName
    );
}
