package com.ocare.backend.health;

import com.ocare.backend.common.ApiResponse;
import com.ocare.backend.health.dto.DailySummaryResponse;
import com.ocare.backend.health.dto.HealthDataUploadRequest;
import com.ocare.backend.health.dto.MonthlySummaryResponse;
import com.ocare.backend.health.entity.HealthDataSource;
import com.ocare.backend.health.service.HealthDataIngestService;
import com.ocare.backend.health.service.HealthSummaryQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 헬스 데이터 저장(수집) / Daily·Monthly 조회 API.
 *
 * POST /api/health-data                         : 삼성헬스/애플건강 → 단말 → 서버로 전달된 원본 payload 저장
 * GET  /api/health-data/{recordkey}/daily        : 특정 일자의 걸음수/칼로리/거리 합계 조회
 * GET  /api/health-data/{recordkey}/monthly      : 특정 월의 걸음수/칼로리/거리 합계 조회
 */
@RestController
@RequestMapping("/api/health-data")
@RequiredArgsConstructor
public class HealthDataController {

    private final HealthDataIngestService healthDataIngestService;
    private final HealthSummaryQueryService healthSummaryQueryService;

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> upload(@Valid @RequestBody HealthDataUploadRequest request) {
        HealthDataSource saved = healthDataIngestService.ingest(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(saved.getId()));
    }

    @GetMapping("/{recordkey}/daily")
    public ResponseEntity<ApiResponse<DailySummaryResponse>> getDaily(
            @PathVariable String recordkey,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.ok(healthSummaryQueryService.getDaily(recordkey, date)));
    }

    @GetMapping("/{recordkey}/monthly")
    public ResponseEntity<ApiResponse<MonthlySummaryResponse>> getMonthly(
            @PathVariable String recordkey,
            @RequestParam String month) {
        return ResponseEntity.ok(ApiResponse.ok(healthSummaryQueryService.getMonthly(recordkey, month)));
    }
}
