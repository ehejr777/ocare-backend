package com.ocare.backend.health.repository;

import com.ocare.backend.health.entity.DailyHealthSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyHealthSummaryRepository extends JpaRepository<DailyHealthSummary, Long> {

    Optional<DailyHealthSummary> findByRecordkeyAndSummaryDate(String recordkey, LocalDate summaryDate);

    List<DailyHealthSummary> findByRecordkeyAndSummaryDateBetweenOrderBySummaryDate(
            String recordkey, LocalDate from, LocalDate to);
}
