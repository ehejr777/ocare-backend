package com.ocare.backend.health.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ocare.backend.common.exception.BusinessException;
import com.ocare.backend.common.exception.ErrorCode;
import com.ocare.backend.health.dto.DailySummaryResponse;
import com.ocare.backend.health.dto.MonthlySummaryResponse;
import com.ocare.backend.health.entity.DailyHealthSummary;
import com.ocare.backend.health.entity.MonthlyHealthSummary;
import com.ocare.backend.health.repository.DailyHealthSummaryRepository;
import com.ocare.backend.health.repository.MonthlyHealthSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;

/**
 * Daily/Monthly 조회 서비스.
 * 동일 recordkey+기간 조회가 반복될 가능성이 높아 Redis 를 조회 캐시(Cache-Aside)로 사용한다.
 *  - cache miss  : DB 조회 후 Redis 에 적재(TTL)
 *  - cache hit   : Redis 값 그대로 반환 (DB 부하 감소)
 *  - 데이터 저장(HealthDataIngestService.ingest) 시점에 캐시를 무효화하여 정합성을 보장한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HealthSummaryQueryService {

    private static final String DAILY_CACHE_PREFIX = "summary:daily:";
    private static final String MONTHLY_CACHE_PREFIX = "summary:monthly:";

    private final DailyHealthSummaryRepository dailyHealthSummaryRepository;
    private final MonthlyHealthSummaryRepository monthlyHealthSummaryRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ocare.cache.summary-ttl-seconds:600}")
    private long summaryTtlSeconds;

    public DailySummaryResponse getDaily(String recordkey, LocalDate date) {
        String cacheKey = DAILY_CACHE_PREFIX + recordkey + ":" + date;

        DailySummaryResponse cached = readCache(cacheKey, DailySummaryResponse.class);
        if (cached != null) {
            log.debug("Daily 요약 조회: cache hit (recordkey={}, date={})", recordkey, date);
            return cached;
        }

        log.debug("Daily 요약 조회: cache miss, DB 조회 중 (recordkey={}, date={})", recordkey, date);
        DailyHealthSummary summary = dailyHealthSummaryRepository
                .findByRecordkeyAndSummaryDate(recordkey, date)
                .orElseThrow(() -> new BusinessException(ErrorCode.HEALTH_DATA_NOT_FOUND));

        DailySummaryResponse response = new DailySummaryResponse(
                summary.getRecordkey(), summary.getSummaryDate(),
                summary.getTotalSteps(), summary.getTotalCalories(), summary.getTotalDistance());

        writeCache(cacheKey, response);
        log.debug("Daily 요약 캐시 저장 (recordkey={}, date={}, ttl={}s)",
                recordkey, date, summaryTtlSeconds);
        return response;
    }

    public MonthlySummaryResponse getMonthly(String recordkey, String yearMonth) {
        String cacheKey = MONTHLY_CACHE_PREFIX + recordkey + ":" + yearMonth;

        MonthlySummaryResponse cached = readCache(cacheKey, MonthlySummaryResponse.class);
        if (cached != null) {
            log.debug("Monthly 요약 조회: cache hit (recordkey={}, month={})", recordkey, yearMonth);
            return cached;
        }

        log.debug("Monthly 요약 조회: cache miss, DB 조회 중 (recordkey={}, month={})", recordkey, yearMonth);
        MonthlyHealthSummary summary = monthlyHealthSummaryRepository
                .findByRecordkeyAndSummaryMonth(recordkey, yearMonth)
                .orElseThrow(() -> new BusinessException(ErrorCode.HEALTH_DATA_NOT_FOUND));

        MonthlySummaryResponse response = new MonthlySummaryResponse(
                summary.getRecordkey(), summary.getSummaryMonth(),
                summary.getTotalSteps(), summary.getTotalCalories(), summary.getTotalDistance());

        writeCache(cacheKey, response);
        log.debug("Monthly 요약 캐시 저장 (recordkey={}, month={}, ttl={}s)",
                recordkey, yearMonth, summaryTtlSeconds);
        return response;
    }

    private <T> T readCache(String key, Class<T> type) {
        try {
            String json = redisTemplate.opsForValue().get(key);
            return json == null ? null : objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("Redis 캐시 조회 실패(무시하고 DB 조회로 진행): {}", e.getMessage());
            return null;
        }
    }

    private void writeCache(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), Duration.ofSeconds(summaryTtlSeconds));
        } catch (Exception e) {
            log.warn("Redis 캐시 저장 실패(무시): {}", e.getMessage());
        }
    }
}
