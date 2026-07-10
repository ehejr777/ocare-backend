package com.ocare.backend.health.dto;

public record MonthlySummaryResponse(String recordkey, String month, Integer steps,
                                      Double calories, Double distance) {
}
