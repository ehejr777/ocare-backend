package com.ocare.backend.health.repository;

import com.ocare.backend.health.entity.MonthlyHealthSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MonthlyHealthSummaryRepository extends JpaRepository<MonthlyHealthSummary, Long> {

    Optional<MonthlyHealthSummary> findByRecordkeyAndSummaryMonth(String recordkey, String summaryMonth);
}
