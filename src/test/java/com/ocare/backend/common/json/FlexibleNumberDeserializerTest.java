package com.ocare.backend.common.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FlexibleNumberDeserializerTest {

    record Sample(@JsonDeserialize(using = FlexibleNumberDeserializer.class) Double steps) {
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 숫자로_내려온_steps를_정상_파싱한다() throws Exception {
        Sample sample = objectMapper.readValue("{\"steps\": 54}", Sample.class);
        assertThat(sample.steps()).isEqualTo(54.0);
    }

    @Test
    void 소수점_문자열로_내려온_steps를_정상_파싱한다() throws Exception {
        Sample sample = objectMapper.readValue("{\"steps\": \"287.6726411513615\"}", Sample.class);
        assertThat(sample.steps()).isEqualTo(287.6726411513615);
    }

    @Test
    void 정수_문자열로_내려온_steps도_정상_파싱한다() throws Exception {
        Sample sample = objectMapper.readValue("{\"steps\": \"8\"}", Sample.class);
        assertThat(sample.steps()).isEqualTo(8.0);
    }
}
