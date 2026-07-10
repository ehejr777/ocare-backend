package com.ocare.backend.health.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ocare.backend.common.json.FlexibleNumberDeserializer;

public record HealthEntryDto(

        @JsonDeserialize(using = FlexibleNumberDeserializer.class)
        Double steps,

        PeriodDto period,
        DistanceDto distance,
        CaloriesDto calories
) {
}
