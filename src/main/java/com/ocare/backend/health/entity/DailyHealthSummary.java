package com.ocare.backend.health.entity;

import com.ocare.backend.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 레코드키(recordkey) 기준 일자별 걸음수/칼로리/거리 집계 테이블.
 * HealthDataIngestService 가 데이터 저장 시점에 upsert 방식으로 갱신한다.
 */
@Getter
@Entity
@Table(name = "daily_health_summary",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_dhs_recordkey_date", columnNames = {"recordkey", "summary_date"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyHealthSummary extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_health_summary_id")
    private Long id;

    @Column(name = "recordkey", nullable = false, length = 64)
    private String recordkey;

    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    /** 걸음수(int) - 필드 스펙 준수, 원본 소수 걸음수 합산 후 반올림 */
    @Column(name = "total_steps", nullable = false)
    private Integer totalSteps;

    /** 소모 칼로리(float) */
    @Column(name = "total_calories", nullable = false)
    private Double totalCalories;

    /** 이동거리(float, km) */
    @Column(name = "total_distance", nullable = false)
    private Double totalDistance;

    public DailyHealthSummary(String recordkey, LocalDate summaryDate, Integer totalSteps,
                               Double totalCalories, Double totalDistance) {
        this.recordkey = recordkey;
        this.summaryDate = summaryDate;
        this.totalSteps = totalSteps;
        this.totalCalories = totalCalories;
        this.totalDistance = totalDistance;
    }

    public void update(Integer totalSteps, Double totalCalories, Double totalDistance) {
        this.totalSteps = totalSteps;
        this.totalCalories = totalCalories;
        this.totalDistance = totalDistance;
    }
}
