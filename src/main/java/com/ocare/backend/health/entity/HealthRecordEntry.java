package com.ocare.backend.health.entity;

import com.ocare.backend.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * entries[] 의 개별 항목(10분 단위 등 구간별 걸음수/거리/칼로리)을 저장하는 원본 테이블.
 * (recordkey, period_from, period_to) 유니크 제약으로 동일 페이로드 재수신 시에도
 * 중복 저장되지 않도록 한다 (본 과제 INPUT_DATA1/INPUT_DATA2 가 완전히 동일한 데이터였음).
 */
@Getter
@Entity
@Table(name = "health_record_entry",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_hre_recordkey_period",
                columnNames = {"recordkey", "period_from", "period_to"}),
        indexes = {
                @Index(name = "idx_hre_recordkey_from", columnList = "recordkey, period_from")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HealthRecordEntry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "health_record_entry_id")
    private Long id;

    @Column(name = "health_data_source_id", nullable = false)
    private Long healthDataSourceId;

    /** 조인 없이 Daily/Monthly 집계 쿼리를 빠르게 하기 위한 비정규화 컬럼 */
    @Column(name = "recordkey", nullable = false, length = 64)
    private String recordkey;

    @Column(name = "period_from", nullable = false)
    private LocalDateTime periodFrom;

    @Column(name = "period_to", nullable = false)
    private LocalDateTime periodTo;

    /** 걸음수. 원본은 소수 문자열로도 내려오나(steps 필드 이슈), 원본 정밀도를 보존하기 위해 double 로 저장 */
    @Column(name = "steps", nullable = false)
    private Double steps;

    @Column(name = "distance_value", nullable = false)
    private Double distanceValue;

    @Column(name = "distance_unit", nullable = false, length = 10)
    private String distanceUnit;

    @Column(name = "calories_value", nullable = false)
    private Double caloriesValue;

    @Column(name = "calories_unit", nullable = false, length = 10)
    private String caloriesUnit;

    public HealthRecordEntry(Long healthDataSourceId, String recordkey, LocalDateTime periodFrom,
                              LocalDateTime periodTo, Double steps, Double distanceValue,
                              String distanceUnit, Double caloriesValue, String caloriesUnit) {
        this.healthDataSourceId = healthDataSourceId;
        this.recordkey = recordkey;
        this.periodFrom = periodFrom;
        this.periodTo = periodTo;
        this.steps = steps;
        this.distanceValue = distanceValue;
        this.distanceUnit = distanceUnit;
        this.caloriesValue = caloriesValue;
        this.caloriesUnit = caloriesUnit;
    }
}
