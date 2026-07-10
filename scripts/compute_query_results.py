"""
Daily/Monthly 조회 결과 산출 스크립트.

sample-data/INPUT_DATA*.json 을 실제 서버 저장 로직(HealthDataIngestService)과 동일한 규칙으로
파싱/중복제거/집계하여 docs/query-results/ 에 CSV 로 산출합니다.

동일 규칙:
  - steps: 문자열/숫자 혼재 -> float 로 파싱 후, 집계 시점에 반올림하여 int 로 표현
  - period.from/to: "yyyy-MM-dd HH:mm:ss" 또는 ISO "yyyy-MM-ddTHH:mm:ss+0000" 두 포맷 모두 지원,
    UTC 기준으로 통일
  - (recordkey, period_from, period_to) 중복 데이터는 1건만 반영 (idempotent upsert 와 동일한 결과)
"""
import json
import csv
from pathlib import Path
from datetime import datetime, timezone
from collections import defaultdict

BASE_DIR = Path(__file__).resolve().parent.parent
SAMPLE_DIR = BASE_DIR / "sample-data"
OUT_DIR = BASE_DIR / "docs" / "query-results"
OUT_DIR.mkdir(parents=True, exist_ok=True)


def parse_period(value: str) -> datetime:
    value = value.strip()
    if "T" in value:
        # 2024-11-14T23:10:00+0000
        dt = datetime.strptime(value, "%Y-%m-%dT%H:%M:%S%z")
        return dt.astimezone(timezone.utc).replace(tzinfo=None)
    # 2024-11-15 00:00:00 (오프셋 없음 -> UTC 로 간주)
    return datetime.strptime(value, "%Y-%m-%d %H:%M:%S")


def parse_steps(value) -> float:
    if isinstance(value, str):
        return float(value)
    return float(value)


def main():
    seen_keys = set()
    daily = defaultdict(lambda: [0.0, 0.0, 0.0])   # (recordkey, date) -> [steps, calories, distance]
    monthly = defaultdict(lambda: [0.0, 0.0, 0.0])  # (recordkey, yyyy-mm) -> [...]

    files = sorted(SAMPLE_DIR.glob("INPUT_DATA*.json"))
    total_entries = 0
    duplicate_entries = 0

    for path in files:
        payload = json.loads(path.read_text(encoding="utf-8"))
        recordkey = payload["recordkey"]
        entries = payload["data"]["entries"]

        for e in entries:
            period_from = parse_period(e["period"]["from"])
            period_to = parse_period(e["period"]["to"])
            dedup_key = (recordkey, period_from, period_to)

            total_entries += 1
            if dedup_key in seen_keys:
                duplicate_entries += 1
                continue
            seen_keys.add(dedup_key)

            steps = parse_steps(e["steps"])
            distance = float(e["distance"]["value"])
            calories = float(e["calories"]["value"])

            date_key = (recordkey, period_from.date().isoformat())
            month_key = (recordkey, period_from.strftime("%Y-%m"))

            daily[date_key][0] += steps
            daily[date_key][1] += calories
            daily[date_key][2] += distance

            monthly[month_key][0] += steps
            monthly[month_key][1] += calories
            monthly[month_key][2] += distance

    # ---- Daily CSV ----
    daily_rows = []
    for (recordkey, date), (steps, calories, distance) in sorted(daily.items(), key=lambda x: (x[0][0], x[0][1])):
        daily_rows.append({
            "recordkey": recordkey,
            "date": date,
            "steps": round(steps),
            "calories": round(calories, 2),
            "distance": round(distance, 4),
        })

    with open(OUT_DIR / "daily_summary.csv", "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["recordkey", "date", "steps", "calories", "distance"])
        writer.writeheader()
        writer.writerows(daily_rows)

    # ---- Monthly CSV ----
    monthly_rows = []
    for (recordkey, month), (steps, calories, distance) in sorted(monthly.items(), key=lambda x: (x[0][0], x[0][1])):
        monthly_rows.append({
            "recordkey": recordkey,
            "month": month,
            "steps": round(steps),
            "calories": round(calories, 2),
            "distance": round(distance, 4),
        })

    with open(OUT_DIR / "monthly_summary.csv", "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=["recordkey", "month", "steps", "calories", "distance"])
        writer.writeheader()
        writer.writerows(monthly_rows)

    print(f"files processed         : {len(files)}")
    print(f"total entries (raw)      : {total_entries}")
    print(f"duplicate entries skipped: {duplicate_entries}")
    print(f"unique entries stored    : {total_entries - duplicate_entries}")
    print(f"daily rows               : {len(daily_rows)}")
    print(f"monthly rows             : {len(monthly_rows)}")


if __name__ == "__main__":
    main()
