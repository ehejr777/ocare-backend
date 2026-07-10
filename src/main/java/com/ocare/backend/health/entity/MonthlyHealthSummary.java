package com.ocare.backend.health.entity;

import com.ocare.backend.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 레코드키(recordkey) 기준 월별 걸음수/칼로리/거리 집계 테이블.
 * summaryMonth 는 "yyyy-MM" 형식 문자열로 저장한다.
 */
@Getter
@Entity
@Table(name = "monthly_health_summary",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_mhs_recordkey_month", columnNames = {"recordkey", "summary_month"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonthlyHealthSummary extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "monthly_health_summary_id")
    private Long id;

    @Column(name = "recordkey", nullable = false, length = 64)
    private String recordkey;

    @Column(name = "summary_month", nullable = false, length = 7)
    private String summaryMonth;

    @Column(name = "total_steps", nullable = false)
    private Integer totalSteps;

    @Column(name = "total_calories", nullable = false)
    private Double totalCalories;

    @Column(name = "total_distance", nullable = false)
    private Double totalDistance;

    public MonthlyHealthSummary(String recordkey, String summaryMonth, Integer totalSteps,
                                 Double totalCalories, Double totalDistance) {
        this.recordkey = recordkey;
        this.summaryMonth = summaryMonth;
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
