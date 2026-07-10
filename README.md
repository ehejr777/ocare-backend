# OCare Backend Developer 채용 과제

삼성헬스/애플건강 등에서 App to App 으로 단말에 전달되고, 단말에서 서버로 수집되는
건강활동 데이터(걸음수/거리/칼로리)를 저장하고, 회원가입/로그인 및 Daily/Monthly
단위 조회 기능을 제공하는 백엔드 서비스입니다.

## 기술 스택

- Java 17
- Spring Boot 3.2 (Web, Data JPA, Data Redis, Security, Validation)
- MySQL 8.x
- Redis (로그인 세션 토큰 / Daily·Monthly 조회 캐시)
- Lombok, JUnit5 + Mockito

## 프로젝트 구조

```
ocare-backend/
├── pom.xml
├── docs/
│   ├── ERD.md                  # ERD (mermaid) + 설계 코멘트
│   ├── schema.sql               # DDL (테이블 설계)
│   └── query-results/           # Daily/Monthly 조회 결과 산출물
│       ├── daily_summary.csv
│       ├── monthly_summary.csv
│       └── query-results.md
├── sample-data/                 # 제공된 INPUT_DATA1~4.json (샘플 적재용)
├── scripts/
│   └── compute_query_results.py # query-results 산출 스크립트 (저장 로직과 동일 규칙)
└── src/
    ├── main/java/com/ocare/backend/
    │   ├── OcareBackendApplication.java
    │   ├── common/                  # 공통 응답/예외/커스텀 Jackson Deserializer
    │   │   ├── ApiResponse.java
    │   │   ├── BaseTimeEntity.java
    │   │   ├── exception/           # BusinessException, ErrorCode, GlobalExceptionHandler
    │   │   └── json/                # FlexibleNumberDeserializer, FlexibleDateTimeDeserializer
    │   ├── config/                  # SecurityConfig, RedisConfig
    │   ├── security/                # LoginSessionService (Redis 세션 토큰)
    │   ├── member/                  # 회원가입/로그인 도메인
    │   │   ├── Member.java / MemberRepository / MemberService / MemberController
    │   │   └── dto/
    │   └── health/                  # 헬스 데이터 저장/조회 도메인
    │       ├── entity/              # HealthDataSource, HealthRecordEntry, Daily/MonthlyHealthSummary
    │       ├── repository/
    │       ├── dto/                 # 입력 JSON과 1:1 매핑되는 DTO
    │       ├── service/             # HealthDataIngestService(저장), HealthSummaryQueryService(조회)
    │       ├── HealthDataController.java
    │       └── SampleDataLoader.java  # sample-data/*.json 적재용 데모 러너 (profile=sample)
    └── test/java/com/ocare/backend/
        ├── common/json/             # Deserializer 단위 테스트
        └── health/service/          # 저장 로직(idempotency) 단위 테스트
```

패키지는 `common`(공통) / `config` / `security` / `member`(회원) / `health`(건강데이터) 로
도메인 기준 분리했습니다. `member` 와 `health` 는 서로 다른 발급 주체의 식별자(로그인 회원 vs
헬스 플랫폼 recordkey)를 다루므로 느슨하게 결합(`health_data_source.member_id` nullable FK)했습니다.

## 실행 방법 (도커 테스트탑 설치필요)

```bash
# 1) MySQL / Redis 준비 (docker 예시)
docker run -d --name ocare-mysql -e MYSQL_DATABASE=ocare -e MYSQL_USER=ocare -e MYSQL_PASSWORD=ocare1234 -e MYSQL_ROOT_PASSWORD=root -p 3306:3306 mysql:8
docker run -d --name ocare-redis -p 6379:6379 redis:7

# 2) 애플리케이션 실행
mvn spring-boot:run

```

## API 요약

| Method | URL | 설명 |
|---|---|---|
| POST | `/api/members/signup` | 회원가입 (이름/닉네임/이메일/패스워드) |
| POST | `/api/members/login` | 로그인 (이메일/패스워드) → 세션 토큰 발급 |
| POST | `/api/health-data` | 헬스 데이터 payload 저장 (INPUT_DATA*.json 형식) |
| GET | `/api/health-data/{recordkey}/daily?date=yyyy-MM-dd` | 일별 걸음수/칼로리/거리 조회 |
| GET | `/api/health-data/{recordkey}/monthly?month=yyyy-MM` | 월별 걸음수/칼로리/거리 조회 |

### 예시 요청 (헬스 데이터 저장)

`POST /api/health-data` 의 Body 는 첨부된 INPUT_DATA*.json 파일과 동일한 구조를 그대로 사용합니다.

```json
{
  "recordkey": "e27ba7ef-8bb2-424c-af1d-877e826b7487",
  "data": {
    "memo": "",
    "entries": [
      {
        "steps": "287.6726411513615",
        "period": { "from": "2024-11-14T23:00:00+0000", "to": "2024-11-14T23:10:00+0000" },
        "distance": { "value": 0.2301381129210892, "unit": "km" },
        "calories": { "value": 0, "unit": "kcal" }
      }
    ]
  },
  "lastUpdate": "2024-12-16 14:40:00 +0000",
  "type": "steps"
}
```

## 필드 설명

| 필드 | 타입 | 설명 |
|---|---|---|
| steps | int (집계 시), 저장 원본은 double | 걸음수 |
| calories | float | 소모 칼로리 (kcal) |
| distance | float | 이동거리 (km) |
| recordkey | varchar | 사용자 구분 키 (삼성헬스/애플건강 등 헬스 플랫폼이 발급) |

## 데이터 조회 결과

첨부된 INPUT_DATA1~4.json 4개 파일을 실제 저장 규칙(중복 제거 포함)으로 집계한 Daily/Monthly
결과는 `docs/query-results/query-results.md`, `daily_summary.csv`, `monthly_summary.csv` 를
참고해 주세요. 요약:

## 발생한 이슈 및 해결 방법

1. **steps 필드 타입 불일치**
   입력 데이터 소스에 따라 `steps` 가 숫자(`54`)로 오거나, 소수점을 포함한 문자열
   (`"287.6726411513615"`)로 오는 두 가지 케이스가 혼재했습니다 (INPUT_DATA1~3은 숫자,
   INPUT_DATA4는 문자열). → `FlexibleNumberDeserializer` 를 작성해 두 타입 모두 안전하게
   `Double` 로 역직렬화하고, 원본은 정밀도를 보존해 저장한 뒤 집계(Daily/Monthly) 시점에만
   `steps - 걸음수(int)` 스펙에 맞춰 반올림했습니다.

2. **period.from/to 날짜 포맷 2종 혼재**
   `"2024-11-15 00:00:00"` (공백 구분, 오프셋 없음) 와 `"2024-11-14T23:10:00+0000"`
   (ISO-8601, +0000 오프셋) 두 형식이 recordkey(데이터 소스)별로 갈려 있었습니다. →
   `FlexibleDateTimeDeserializer` 로 두 포맷을 모두 파싱하고 UTC 기준 `LocalDateTime` 으로
   통일해 저장했습니다. 오프셋이 없는 포맷은 동일 payload 내 `lastUpdate` 등 다른 시각
   필드들이 UTC로 취급되고 있다고 보고 동일하게 UTC 로 간주했습니다 (실제 서비스라면 데이터
   제공사에 포맷 표준화를 요청하는 것이 우선입니다).

3. **lastUpdate 필드의 제3의 포맷**
   `"2024-12-16 14:40:00 +0000"` 처럼 공백 구분 + 오프셋이 함께 있는 세 번째 포맷이 메타
   정보에 사용되었습니다. → 해당 필드는 집계 로직에 영향이 없는 부가 정보이므로 별도
   포맷터로 관대하게 파싱하고, 실패 시에도 저장 자체는 막지 않도록(경고 로그만 남기고
   null 저장) 했습니다.

4. **동일 payload 재수신 시 중복 저장 방지**
   단말 재동기화/네트워크 재시도로 동일 payload 가 여러 번 전송될 수 있다고 판단해,
   `health_record_entry` 에 `(recordkey, period_from, period_to)` 유니크 제약을 걸고
   저장 전 존재 여부를 확인하는 idempotent 저장 로직을 구현했습니다.

5. **Daily/Monthly 조회 성능**
   원본 entries 테이블에서 매 조회마다 집계하면 데이터가 누적될수록 느려지므로,
   저장(ingest) 시점에 영향을 받은 일자/월만 선택적으로 재집계해 `daily_health_summary` /
   `monthly_health_summary` 테이블에 upsert 해두고, 조회는 이 요약 테이블에서 수행합니다.
   또한 반복 조회가 잦을 것으로 예상해 Redis 를 Cache-Aside 로 얹어 DB 부하를 줄였습니다.

## 설계상 가정 (평가 시 참고)

- 회원가입/로그인은 이름·닉네임·이메일·패스워드 CRUD 수준으로 구현했고, 헬스 데이터
  저장/조회 API 는 별도 인증 없이 permitAll 로 열어두었습니다 (과제 명세에 보호 API 요건이
  명시되어 있지 않아 범위를 벗어난 과설계를 피했습니다). 로그인 시 Redis 기반 세션 토큰은
  발급하며, 실제 서비스라면 이 토큰을 헬스 데이터 API 인증에도 재사용할 수 있습니다.
- 비밀번호는 BCrypt 로 암호화하여 저장합니다.

