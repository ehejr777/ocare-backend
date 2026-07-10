-- =========================================================
-- OCare Backend 과제 - DB 스키마 (MySQL 8.x)
-- Spring Data JPA 의 ddl-auto=update 로 자동 생성되는 스키마와 동일하며,
-- 최초 배포 시 검토/실행용으로 별도 제공합니다.
-- =========================================================

CREATE DATABASE IF NOT EXISTS ocare DEFAULT CHARACTER SET utf8mb4;
USE ocare;

-- ---------------------------------------------------------
-- 1. member : 회원 (회원가입/로그인)
-- ---------------------------------------------------------
CREATE TABLE member (
    member_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL COMMENT '이름',
    nickname    VARCHAR(30)  NOT NULL COMMENT '닉네임',
    email       VARCHAR(100) NOT NULL COMMENT '이메일 (로그인 ID)',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 암호화된 비밀번호',
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    CONSTRAINT uk_member_email    UNIQUE (email),
    CONSTRAINT uk_member_nickname UNIQUE (nickname)
) COMMENT '회원';

-- ---------------------------------------------------------
-- 2. health_data_source : 수신된 헬스 데이터 payload(전송 단위) 메타
--    예) INPUT_DATA1.json ~ INPUT_DATA4.json 각 1건
-- ---------------------------------------------------------
CREATE TABLE health_data_source (
    health_data_source_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recordkey             VARCHAR(64)  NOT NULL COMMENT '사용자 구분 키 (삼성헬스/애플건강 발급)',
    type                   VARCHAR(20)  COMMENT '데이터 종류 (예: steps)',
    memo                   VARCHAR(255) COMMENT '메모',
    external_last_update   DATETIME(6)  COMMENT '헬스 플랫폼 마지막 동기화 시각(UTC)',
    member_id              BIGINT       COMMENT '연동된 회원 (선택, nullable)',
    created_at             DATETIME(6)  NOT NULL,
    updated_at             DATETIME(6)  NOT NULL,
    KEY idx_hds_recordkey (recordkey),
    CONSTRAINT fk_hds_member FOREIGN KEY (member_id) REFERENCES member (member_id)
) COMMENT '헬스 데이터 수신 payload 메타';

-- ---------------------------------------------------------
-- 3. health_record_entry : entries[] 원본 저장 (구간별 걸음수/거리/칼로리)
-- ---------------------------------------------------------
CREATE TABLE health_record_entry (
    health_record_entry_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    health_data_source_id  BIGINT       NOT NULL,
    recordkey              VARCHAR(64)  NOT NULL COMMENT '사용자 구분 키 (비정규화, 집계쿼리 성능용)',
    period_from             DATETIME(6)  NOT NULL COMMENT '구간 시작시각(UTC)',
    period_to               DATETIME(6)  NOT NULL COMMENT '구간 종료시각(UTC)',
    steps                   DOUBLE       NOT NULL COMMENT '걸음수 (원본 정밀도 보존, 집계 시 int 반올림)',
    distance_value          DOUBLE       NOT NULL COMMENT '이동거리 값',
    distance_unit           VARCHAR(10)  NOT NULL COMMENT '이동거리 단위 (km)',
    calories_value          DOUBLE       NOT NULL COMMENT '소모 칼로리 값',
    calories_unit           VARCHAR(10)  NOT NULL COMMENT '칼로리 단위 (kcal)',
    created_at              DATETIME(6)  NOT NULL,
    updated_at              DATETIME(6)  NOT NULL,
    CONSTRAINT uk_hre_recordkey_period UNIQUE (recordkey, period_from, period_to),
    KEY idx_hre_recordkey_from (recordkey, period_from),
    CONSTRAINT fk_hre_source FOREIGN KEY (health_data_source_id) REFERENCES health_data_source (health_data_source_id)
) COMMENT '헬스 데이터 원본 구간 엔트리';

-- ---------------------------------------------------------
-- 4. daily_health_summary : 레코드키 기준 일별 집계 (조회 성능/캐시 원본)
-- ---------------------------------------------------------
CREATE TABLE daily_health_summary (
    daily_health_summary_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recordkey                VARCHAR(64) NOT NULL,
    summary_date              DATE        NOT NULL,
    total_steps                INT         NOT NULL COMMENT '걸음수(int)',
    total_calories             DOUBLE      NOT NULL COMMENT '소모 칼로리(float)',
    total_distance             DOUBLE      NOT NULL COMMENT '이동거리(float, km)',
    created_at                 DATETIME(6) NOT NULL,
    updated_at                 DATETIME(6) NOT NULL,
    CONSTRAINT uk_dhs_recordkey_date UNIQUE (recordkey, summary_date)
) COMMENT '일별 걸음수/칼로리/거리 집계';

-- ---------------------------------------------------------
-- 5. monthly_health_summary : 레코드키 기준 월별 집계
-- ---------------------------------------------------------
CREATE TABLE monthly_health_summary (
    monthly_health_summary_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    recordkey                  VARCHAR(64) NOT NULL,
    summary_month                VARCHAR(7)  NOT NULL COMMENT 'yyyy-MM',
    total_steps                  INT         NOT NULL,
    total_calories               DOUBLE      NOT NULL,
    total_distance               DOUBLE      NOT NULL,
    created_at                   DATETIME(6) NOT NULL,
    updated_at                   DATETIME(6) NOT NULL,
    CONSTRAINT uk_mhs_recordkey_month UNIQUE (recordkey, summary_month)
) COMMENT '월별 걸음수/칼로리/거리 집계';
