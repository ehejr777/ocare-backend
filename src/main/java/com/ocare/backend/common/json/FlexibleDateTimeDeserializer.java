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
 * 해결: 두 포맷을 모두 시도하여 파싱하고, 서버 내부 저장 기준시(Asia/Seoul)로 통일한다.
 * ISO-8601 포맷의 오프셋은 제거하고 LocalDateTime 시간값만 보존한다.
 * application.yml에서 Asia/Seoul로 설정되어 있으므로, 모든 시각 필드는 Asia/Seoul로 취급된다.
 */
public class FlexibleDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getText();
        if (text == null || text.isBlank()) {
            return null;
        }
        text = text.trim();

        try {
            if (text.indexOf('T') >= 0) {
                // +0000 형식을 +00:00으로 변환 (표준 ISO-8601)
                if (text.matches(".*[+-]\\d{4}$")) {
                    text = text.substring(0, text.length() - 2) + ":" + text.substring(text.length() - 2);
                }

                OffsetDateTime odt = OffsetDateTime.parse(text, DateTimeFormatterProvider.PERIOD_ISO_WITH_OFFSET);
                // Asia/Seoul 기준: 오프셋만 제거하고 LocalDateTime 변환 (시간값 보존)
                return odt.toLocalDateTime();
            }
            return LocalDateTime.parse(text, DateTimeFormatterProvider.PERIOD_SPACE_SEPARATED);
        } catch (Exception e) {
            throw e;
        }
    }
}
