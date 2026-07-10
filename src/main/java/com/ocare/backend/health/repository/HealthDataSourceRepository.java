package com.ocare.backend.health.repository;

import com.ocare.backend.health.entity.HealthDataSource;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HealthDataSourceRepository extends JpaRepository<HealthDataSource, Long> {
}
