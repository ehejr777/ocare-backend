package com.ocare.backend.health.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.LocalDateTime;
import java.util.List;

/**
 * INPUT_DATA*.json (삼성헬스/애플건강 → 단말 → 서버 수집 payload) 요청 형태와 1:1 매핑되는 DTO.
 * 예)
 * {
 *   "recordkey": "e27ba7ef-...",
 *   "data": { "memo": "", "entries": [ {...}, {...} ] },
 *   "lastUpdate": "2024-12-16 14:40:00 +0000",
 *   "type": "steps"
 * }
 */
public record HealthDataUploadRequest(

        @NotBlank(message = "recordkey는 필수입니다.")
        String recordkey,

        @Valid
        DataDto data,

        String lastUpdate,

        String type
) {
    public record DataDto(
            String memo,

            @NotEmpty(message = "entries는 최소 1건 이상이어야 합니다.")
            List<@Valid HealthEntryDto> entries
    ) {
    }
}
