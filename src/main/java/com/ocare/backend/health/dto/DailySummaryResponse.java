package com.ocare.backend.health.dto;

import java.time.LocalDate;

public record DailySummaryResponse(String recordkey, LocalDate date, Integer steps,
                                    Double calories, Double distance) {
}
