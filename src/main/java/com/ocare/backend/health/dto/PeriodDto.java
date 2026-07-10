package com.ocare.backend.health.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.ocare.backend.common.json.FlexibleDateTimeDeserializer;

import java.time.LocalDateTime;

public record PeriodDto(

        @JsonDeserialize(using = FlexibleDateTimeDeserializer.class)
        LocalDateTime from,

        @JsonDeserialize(using = FlexibleDateTimeDeserializer.class)
        LocalDateTime to
) {
}
