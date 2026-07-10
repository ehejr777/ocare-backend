package com.ocare.backend.health.service;

import com.ocare.backend.common.exception.BusinessException;
import com.ocare.backend.common.exception.ErrorCode;
import com.ocare.backend.health.dto.HealthDataUploadRequest;
import com.ocare.backend.health.dto.HealthEntryDto;
import com.ocare.backend.health.entity.DailyHealthSummary;
import com.ocare.backend.health.entity.HealthDataSource;
import com.ocare.backend.health.entity.HealthRecordEntry;
import com.ocare.backend.health.entity.MonthlyHealthSummary;
import com.ocare.backend.health.repository.DailyHealthSummaryRepository;
import com.ocare.backend.health.repository.HealthAggregateProjection;
import com.ocare.backend.health.repository.HealthDataSourceRepository;
import com.ocare.backend.health.repository.HealthRecordEntryRepository;
import com.ocare.backend.health.repository.MonthlyHealthSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.HashSet;
import java.util.Set;

/**
 * 헬스 데이터 저장(수집) 서비스.
 *
 * 처리 흐름:
 *  1) HealthDataSource(수신 payload 메타) 저장
 *  2) entries[] 를 HealthRecordEntry 로 저장 (recordkey+period 유니크 → 중복 재수신 시 idempotent)
 *  3) 신규 저장된 entry 들이 걸쳐있는 일자/월에 대해 Daily/MonthlyHealthSummary 재집계(upsert)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthDataIngestService {

    // "2024-12-16 14:40:00 +0000" 형태의 lastUpdate 전용 포맷 (공백 + 오프셋)
    private static final DateTimeFormatter LAST_UPDATE_FORMAT =
            new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd HH:mm:ss Z").toFormatter();

    private final HealthDataSourceRepository healthDataSourceRepository;
    private final HealthRecordEntryRepository healthRecordEntryRepository;
    private final DailyHealthSummaryRepository dailyHealthSummaryRepository;
    private final MonthlyHealthSummaryRepository monthlyHealthSummaryRepository;

    @Transactional
    public HealthDataSource ingest(HealthDataUploadRequest request) {
        return ingest(request, null);
    }

    @Transactional
    public HealthDataSource ingest(HealthDataUploadRequest request, Long memberId) {
        if (request.data() == null || request.data().entries() == null || request.data().entries().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_HEALTH_DATA, "entries 가 비어있습니다.");
        }

        LocalDateTime externalLastUpdate = parseLastUpdate(request.lastUpdate());

        HealthDataSource source = new HealthDataSource(
                request.recordkey(),
                request.type(),
                request.data().memo(),
                externalLastUpdate,
                memberId
        );
        source = healthDataSourceRepository.save(source);

        Set<LocalDate> touchedDates = new HashSet<>();

        for (HealthEntryDto entry : request.data().entries()) {
            boolean alreadyExists = healthRecordEntryRepository
                    .findByRecordkeyAndPeriodFromAndPeriodTo(
                            request.recordkey(), entry.period().from(), entry.period().to())
                    .isPresent();

            if (alreadyExists) {
                // 동일 recordkey/period 데이터가 이미 저장되어 있으면 재수신된 중복 데이터로 간주하고 skip
                continue;
            }

            HealthRecordEntry record = new HealthRecordEntry(
                    source.getId(),
                    request.recordkey(),
                    entry.period().from(),
                    entry.period().to(),
                    entry.steps(),
                    entry.distance() != null ? entry.distance().value() : 0.0,
                    entry.distance() != null ? entry.distance().unit() : "km",
                    entry.calories() != null ? entry.calories().value() : 0.0,
                    entry.calories() != null ? entry.calories().unit() : "kcal"
            );
            healthRecordEntryRepository.save(record);
            touchedDates.add(entry.period().from().toLocalDate());
        }

        recomputeSummaries(request.recordkey(), touchedDates);

        return source;
    }

    private LocalDateTime parseLastUpdate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return java.time.OffsetDateTime.parse(raw.trim(), LAST_UPDATE_FORMAT)
                    .withOffsetSameInstant(java.time.ZoneOffset.UTC)
                    .toLocalDateTime();
        } catch (Exception e) {
            log.warn("lastUpdate 파싱 실패, null 로 저장합니다. raw={}", raw);
            return null;
        }
    }

    /** 신규 저장분이 걸쳐있는 일자(및 그 달)에 대해서만 재집계하여 불필요한 전체 재계산을 피한다. */
    private void recomputeSummaries(String recordkey, Set<LocalDate> touchedDates) {
        Set<String> touchedMonths = new HashSet<>();

        for (LocalDate date : touchedDates) {
            recomputeDaily(recordkey, date);
            touchedMonths.add(date.format(DateTimeFormatter.ofPattern("yyyy-MM")));
        }
        for (String month : touchedMonths) {
            recomputeMonthly(recordkey, month);
        }
    }

    private void recomputeDaily(String recordkey, LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();

        HealthAggregateProjection agg = healthRecordEntryRepository.aggregate(recordkey, from, to);
        int totalSteps = (int) Math.round(agg.getTotalSteps());

        DailyHealthSummary summary = dailyHealthSummaryRepository
                .findByRecordkeyAndSummaryDate(recordkey, date)
                .orElse(null);

        if (summary == null) {
            dailyHealthSummaryRepository.save(new DailyHealthSummary(
                    recordkey, date, totalSteps, agg.getTotalCalories(), agg.getTotalDistance()));
        } else {
            summary.update(totalSteps, agg.getTotalCalories(), agg.getTotalDistance());
        }
    }

    private void recomputeMonthly(String recordkey, String yearMonth) {
        LocalDate monthStart = LocalDate.parse(yearMonth + "-01");
        LocalDate monthEnd = monthStart.plusMonths(1);

        HealthAggregateProjection agg = healthRecordEntryRepository.aggregate(
                recordkey, monthStart.atStartOfDay(), monthEnd.atStartOfDay());
        int totalSteps = (int) Math.round(agg.getTotalSteps());

        MonthlyHealthSummary summary = monthlyHealthSummaryRepository
                .findByRecordkeyAndSummaryMonth(recordkey, yearMonth)
                .orElse(null);

        if (summary == null) {
            monthlyHealthSummaryRepository.save(new MonthlyHealthSummary(
                    recordkey, yearMonth, totalSteps, agg.getTotalCalories(), agg.getTotalDistance()));
        } else {
            summary.update(totalSteps, agg.getTotalCalories(), agg.getTotalDistance());
        }
    }
}
