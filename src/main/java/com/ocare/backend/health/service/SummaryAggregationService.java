package com.ocare.backend.health.service;

import com.ocare.backend.health.entity.DailyHealthSummary;
import com.ocare.backend.health.entity.HealthRecordEntry;
import com.ocare.backend.health.entity.MonthlyHealthSummary;
import com.ocare.backend.health.repository.DailyHealthSummaryRepository;
import com.ocare.backend.health.repository.HealthRecordEntryRepository;
import com.ocare.backend.health.repository.MonthlyHealthSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;

/**
 * 헬스 데이터 요약(Daily/Monthly Summary) 재계산 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryAggregationService {

    private final HealthRecordEntryRepository healthRecordEntryRepository;
    private final DailyHealthSummaryRepository dailyHealthSummaryRepository;
    private final MonthlyHealthSummaryRepository monthlyHealthSummaryRepository;

    /**
     * 특정 recordkey에 대해 주어진 날짜들의 Daily/Monthly 요약을 재계산
     *
     * @param recordkey 레코드 키
     * @param touchedDates 영향받은 날짜들
     */
    @Transactional
    public void recomputeSummaries(String recordkey, Set<LocalDate> touchedDates) {
        if (touchedDates == null || touchedDates.isEmpty()) {
            log.debug("요약 재계산 스킵: 영향받은 날짜가 없습니다. recordkey={}", recordkey);
            return;
        }

        log.info("요약 재계산 시작: recordkey={}, touched_dates_count={}", recordkey, touchedDates.size());

        // 일일 요약 재계산
        for (LocalDate date : touchedDates) {
            recomputeDailySummary(recordkey, date);
        }

        // 월간 요약 재계산 (영향받은 날짜들이 포함된 월들)
        Set<YearMonth> touchedMonths = extractMonths(touchedDates);
        for (YearMonth month : touchedMonths) {
            recomputeMonthlySummary(recordkey, month);
        }

        log.info("요약 재계산 완료: recordkey={}", recordkey);
    }

    /**
     * 특정 날짜의 Daily 요약 재계산
     */
    private void recomputeDailySummary(String recordkey, LocalDate date) {
        List<HealthRecordEntry> entries = healthRecordEntryRepository
                .findByRecordkeyAndDateRange(recordkey, date);

        double totalSteps = 0;
        double totalDistance = 0;
        double totalCalories = 0;

        for (HealthRecordEntry entry : entries) {
            totalSteps += entry.getSteps();
            totalDistance += entry.getDistanceValue();
            totalCalories += entry.getCaloriesValue();
        }

        // 기존 요약 조회 또는 새로 생성
        DailyHealthSummary summary = dailyHealthSummaryRepository
                .findByRecordkeyAndSummaryDate(recordkey, date)
                .orElseGet(() -> new DailyHealthSummary(recordkey, date, 0, 0.0, 0.0));

        summary.update((int) totalSteps, totalCalories, totalDistance);

        dailyHealthSummaryRepository.save(summary);
        log.debug("Daily 요약 저장: recordkey={}, date={}, steps={}, distance={}, calories={}",
                recordkey, date, totalSteps, totalDistance, totalCalories);
    }

    /**
     * 특정 월의 Monthly 요약 재계산
     */
    private void recomputeMonthlySummary(String recordkey, YearMonth month) {
        LocalDate startDate = month.atDay(1);
        LocalDate endDate = month.atEndOfMonth();

        List<HealthRecordEntry> entries = healthRecordEntryRepository
                .findByRecordkeyAndDateRangeBetween(recordkey, startDate, endDate);

        double totalSteps = 0;
        double totalDistance = 0;
        double totalCalories = 0;

        for (HealthRecordEntry entry : entries) {
            totalSteps += entry.getSteps();
            totalDistance += entry.getDistanceValue();
            totalCalories += entry.getCaloriesValue();
        }

        String monthStr = month.toString(); // "yyyy-MM" 형식

        // 기존 요약 조회 또는 새로 생성
        MonthlyHealthSummary summary = monthlyHealthSummaryRepository
                .findByRecordkeyAndSummaryMonth(recordkey, monthStr)
                .orElseGet(() -> new MonthlyHealthSummary(recordkey, monthStr, 0, 0.0, 0.0));

        summary.update((int) totalSteps, totalCalories, totalDistance);

        monthlyHealthSummaryRepository.save(summary);
        log.debug("Monthly 요약 저장: recordkey={}, month={}, steps={}, distance={}, calories={}",
                recordkey, month, totalSteps, totalDistance, totalCalories);
    }

    /**
     * LocalDate Set을 YearMonth Set으로 변환
     */
    private Set<YearMonth> extractMonths(Set<LocalDate> dates) {
        return dates.stream()
                .map(YearMonth::from)
                .collect(java.util.stream.Collectors.toSet());
    }
}
