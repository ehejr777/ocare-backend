package com.ocare.backend.health.repository;

import com.ocare.backend.health.entity.HealthRecordEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HealthRecordEntryRepository extends JpaRepository<HealthRecordEntry, Long> {

    Optional<HealthRecordEntry> findByRecordkeyAndPeriodFromAndPeriodTo(
            String recordkey, LocalDateTime periodFrom, LocalDateTime periodTo);

    @Query("select e from HealthRecordEntry e " +
            "where e.recordkey = :recordkey " +
            "and e.periodFrom >= :from and e.periodFrom < :to " +
            "order by e.periodFrom")
    List<HealthRecordEntry> findAllInRange(@Param("recordkey") String recordkey,
                                            @Param("from") LocalDateTime from,
                                            @Param("to") LocalDateTime to);

    /** 특정 recordkey 데이터가 걸쳐있는 일자 목록 (재집계 대상 파악용) */
    @Query("select distinct function('date', e.periodFrom) from HealthRecordEntry e " +
            "where e.recordkey = :recordkey")
    List<java.sql.Date> findDistinctDatesByRecordkey(@Param("recordkey") String recordkey);

    @Query("select coalesce(sum(e.steps), 0.0) as totalSteps, " +
            "coalesce(sum(e.caloriesValue), 0.0) as totalCalories, " +
            "coalesce(sum(e.distanceValue), 0.0) as totalDistance " +
            "from HealthRecordEntry e " +
            "where e.recordkey = :recordkey and e.periodFrom >= :from and e.periodFrom < :to")
    HealthAggregateProjection aggregate(@Param("recordkey") String recordkey,
                                         @Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);
}
