package com.ocare.backend.common.time;

import java.time.format.DateTimeFormatter;

/**
 * 날짜/시간 포매터를 제공하는 유틸리티 클래스
 */
public class DateTimeFormatterProvider {

    /**
     * lastUpdate 필드 파싱용 포매터 (ISO 8601 OffsetDateTime)
     */
    public static final DateTimeFormatter LAST_UPDATE =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /**
     * 일반적인 날짜 포매터 (yyyy-MM-dd)
     */
    public static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 일반적인 날짜-시간 포매터 (yyyy-MM-dd HH:mm:ss)
     */
    public static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Period 필드 ISO-8601 포매터 (오프셋 포함)
     * 예: 2024-12-16T14:40:00+0000
     */
    public static final DateTimeFormatter PERIOD_ISO_WITH_OFFSET =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /**
     * Period 필드 공백 구분 포매터 (오프셋 없음)
     * 예: 2024-12-16 14:40:00
     */
    public static final DateTimeFormatter PERIOD_SPACE_SEPARATED =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
}
