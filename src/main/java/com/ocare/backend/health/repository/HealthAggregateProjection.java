package com.ocare.backend.health.repository;

/** Daily/Monthly 집계 쿼리 결과를 담기 위한 인터페이스 기반 Projection (Spring Data JPA). */
public interface HealthAggregateProjection {
    Double getTotalSteps();
    Double getTotalCalories();
    Double getTotalDistance();
}
