package com.ocare.backend.common.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class FlexibleDateTimeDeserializerTest {

    record Sample(@JsonDeserialize(using = FlexibleDateTimeDeserializer.class) LocalDateTime value) {
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 공백구분_포맷을_파싱한다() throws Exception {
        Sample sample = objectMapper.readValue("{\"value\": \"2024-11-15 00:10:00\"}", Sample.class);
        assertThat(sample.value()).isEqualTo(LocalDateTime.of(2024, 11, 15, 0, 10, 0));
    }

    @Test
    void ISO_오프셋_포맷을_UTC로_변환하여_파싱한다() throws Exception {
        Sample sample = objectMapper.readValue("{\"value\": \"2024-11-14T23:10:00+0000\"}", Sample.class);
        assertThat(sample.value()).isEqualTo(LocalDateTime.of(2024, 11, 14, 23, 10, 0));
    }
}
