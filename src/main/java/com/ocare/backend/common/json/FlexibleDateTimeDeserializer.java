package com.ocare.backend.common.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.ocare.backend.common.time.DateTimeFormatterProvider;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * 입력 데이터 이슈 대응용 커스텀 Deserializer.
 *
 * 문제: period.from / period.to 필드가 소스에 따라 두 가지 포맷으로 내려온다.
 *   1) "yyyy-MM-dd HH:mm:ss"          (공백 구분, 오프셋 없음)          - 예: INPUT_DATA1~3
 *   2) "yyyy-MM-dd'T'HH:mm:ssZ"       (ISO-8601, +0000 오프셋 포함)     - 예: INPUT_DATA4
 *
 * 해결: 두 포맷을 모두 시도하여 파싱하고, 서버 내부 저장 기준시(UTC)로 통일한다.
 * 오프셋이 없는 포맷(1번)은 실제 관측 결과 lastUpdate 등 동일 payload 내 다른
 * 시각 필드가 UTC 로 취급되고 있어, 동일하게 UTC 로 간주한다 (README 이슈 항목 참고).
 */
public class FlexibleDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        if (text == null || text.isBlank()) {
            return null;
        }
        text = text.trim();

        if (text.indexOf('T') >= 0) {
            OffsetDateTime odt = OffsetDateTime.parse(text, DateTimeFormatterProvider.PERIOD_ISO_WITH_OFFSET);
            return odt.withOffsetSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime();
        }
        return LocalDateTime.parse(text, DateTimeFormatterProvider.PERIOD_SPACE_SEPARATED);
    }
}
