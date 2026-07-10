package com.ocare.backend.health.service;

import com.ocare.backend.health.dto.*;
import com.ocare.backend.health.entity.HealthDataSource;
import com.ocare.backend.health.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HealthDataIngestServiceTest {

    @Mock
    private HealthDataSourceRepository healthDataSourceRepository;
    @Mock
    private HealthRecordEntryRepository healthRecordEntryRepository;
    @Mock
    private DailyHealthSummaryRepository dailyHealthSummaryRepository;
    @Mock
    private MonthlyHealthSummaryRepository monthlyHealthSummaryRepository;

    @InjectMocks
    private HealthDataIngestService ingestService;

    private HealthDataUploadRequest request;

    @BeforeEach
    void setUp() {
        HealthEntryDto entry = new HealthEntryDto(
                100.0,
                new PeriodDto(LocalDateTime.of(2024, 11, 15, 0, 0), LocalDateTime.of(2024, 11, 15, 0, 10)),
                new DistanceDto(0.08, "km"),
                new CaloriesDto(3.5, "kcal")
        );
        HealthDataUploadRequest.DataDto data = new HealthDataUploadRequest.DataDto("", List.of(entry));
        request = new HealthDataUploadRequest("recordkey-1", data, "2024-12-16 14:40:00 +0000", "steps");
    }

    @Test
    void 신규_entry는_저장된다() {
        when(healthDataSourceRepository.save(any())).thenAnswer(inv -> {
            HealthDataSource src = inv.getArgument(0);
            return src; // id 는 null 이지만 단위 테스트 목적상 그대로 반환
        });
        when(healthRecordEntryRepository.findByRecordkeyAndPeriodFromAndPeriodTo(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(healthRecordEntryRepository.aggregate(any(), any(), any()))
                .thenReturn(new HealthAggregateProjection() {
                    public Double getTotalSteps() { return 100.0; }
                    public Double getTotalCalories() { return 3.5; }
                    public Double getTotalDistance() { return 0.08; }
                });
        when(dailyHealthSummaryRepository.findByRecordkeyAndSummaryDate(any(), any())).thenReturn(Optional.empty());
        when(monthlyHealthSummaryRepository.findByRecordkeyAndSummaryMonth(any(), any())).thenReturn(Optional.empty());

        ingestService.ingest(request);

        verify(healthRecordEntryRepository, times(1)).save(any());
        verify(dailyHealthSummaryRepository, times(1)).save(any());
        verify(monthlyHealthSummaryRepository, times(1)).save(any());
    }

    @Test
    void 이미_존재하는_recordkey_period_조합은_중복저장하지_않는다() {
        // entry 가 이미 존재하면 touchedDates 가 비어있게 되어 Daily/Monthly 재집계 자체가 호출되지 않는다.
        when(healthDataSourceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(healthRecordEntryRepository.findByRecordkeyAndPeriodFromAndPeriodTo(any(), any(), any()))
                .thenReturn(Optional.of(mock(com.ocare.backend.health.entity.HealthRecordEntry.class)));

        ingestService.ingest(request);

        verify(healthRecordEntryRepository, never()).save(any());
        verify(dailyHealthSummaryRepository, never()).save(any());
        verify(monthlyHealthSummaryRepository, never()).save(any());
    }
}
