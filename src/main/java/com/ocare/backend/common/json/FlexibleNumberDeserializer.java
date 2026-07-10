package com.ocare.backend.common.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * 입력 데이터 이슈 대응용 커스텀 Deserializer.
 *
 * 문제: 삼성헬스/애플건강 등 데이터 소스에 따라 steps 필드가
 *   - 숫자로 오는 경우 (54, 312 ...)
 *   - 소수점을 포함한 문자열로 오는 경우 ("287.6726411513615", "8" ...)
 * 두 가지 형태로 혼재되어 내려온다 (INPUT_DATA1~3 은 숫자, INPUT_DATA4 는 문자열).
 *
 * 해결: 값의 JSON 타입에 관계없이 문자열/숫자 모두 안전하게 Double 로 파싱한다.
 * 최종적으로 걸음수(steps)는 스펙상 정수(int) 이므로, 집계 시점에 반올림하여 사용한다.
 */
public class FlexibleNumberDeserializer extends JsonDeserializer<Double> {

    @Override
    public Double deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken token = p.currentToken();
        if (token == JsonToken.VALUE_STRING) {
            String text = p.getText();
            if (text == null || text.isBlank()) {
                return 0.0;
            }
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException e) {
                throw new IOException("숫자로 변환할 수 없는 값입니다: " + text, e);
            }
        }
        // 숫자 토큰(정수/실수) 인 경우
        return p.getValueAsDouble();
    }
}
