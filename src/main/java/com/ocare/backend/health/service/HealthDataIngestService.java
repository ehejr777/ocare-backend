package com.ocare.backend.health.service;

import com.ocare.backend.common.exception.BusinessException;
import com.ocare.backend.common.exception.ErrorCode;
import com.ocare.backend.common.time.DateTimeFormatterProvider;
import com.ocare.backend.health.dto.HealthDataUploadRequest;
import com.ocare.backend.health.dto.HealthEntryDto;
import com.ocare.backend.health.entity.HealthDataSource;
import com.ocare.backend.health.entity.HealthRecordEntry;
import com.ocare.backend.health.repository.HealthDataSourceRepository;
import com.ocare.backend.health.repository.HealthRecordEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 헬스 데이터 저장(수집) 서비스.
 *
 * 처리 흐름:
 *  1) HealthDataSource(수신 payload 메타) 저장
 *  2) entries[] 를 HealthRecordEntry 로 저장 (recordkey+period 유니크 → 중복 재수신 시 idempotent)
 *  3) SummaryAggregationService 에 위임하여 Daily/MonthlyHealthSummary 재집계
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthDataIngestService {

    private final HealthDataSourceRepository healthDataSourceRepository;
    private final HealthRecordEntryRepository healthRecordEntryRepository;
    private final SummaryAggregationService summaryAggregationService;

    @Transactional
    public HealthDataSource ingest(HealthDataUploadRequest request) {
        return ingest(request, null);
    }

    @Transactional
    public HealthDataSource ingest(HealthDataUploadRequest request, Long memberId) {
        log.info("헬스 데이터 저장 시작: recordkey={}, type={}, entry_count={}, memberId={}",
                request.recordkey(), request.type(), request.data().entries().size(), memberId);

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
        log.debug("HealthDataSource 저장됨: id={}, recordkey={}", source.getId(), source.getRecordkey());

        Set<LocalDate> touchedDates = new HashSet<>();
        int savedCount = 0;
        int duplicateCount = 0;

        for (HealthEntryDto entry : request.data().entries()) {
            boolean alreadyExists = healthRecordEntryRepository
                    .findByRecordkeyAndPeriodFromAndPeriodTo(
                            request.recordkey(), entry.period().from(), entry.period().to())
                    .isPresent();

            if (alreadyExists) {
                duplicateCount++;
                log.debug("중복 entry 감지 (skip): recordkey={}, period={}~{}",
                        request.recordkey(), entry.period().from(), entry.period().to());
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
            savedCount++;
        }

        log.debug("HealthRecordEntry 저장 완료: saved={}, duplicate={}, touched_dates={}",
                savedCount, duplicateCount, touchedDates.size());

        // 요약 재계산은 SummaryAggregationService에 위임
        summaryAggregationService.recomputeSummaries(request.recordkey(), touchedDates);

        log.info("헬스 데이터 저장 완료: recordkey={}, saved_entries={}, duplicate_entries={}, touched_dates={}",
                request.recordkey(), savedCount, duplicateCount, touchedDates.size());

        return source;
    }

    private LocalDateTime parseLastUpdate(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return java.time.OffsetDateTime.parse(raw.trim(), DateTimeFormatterProvider.LAST_UPDATE)
                    .withOffsetSameInstant(java.time.ZoneOffset.UTC)
                    .toLocalDateTime();
        } catch (Exception e) {
            log.warn("lastUpdate 파싱 실패, null 로 저장합니다. raw={}", raw);
            return null;
        }
    }
}
