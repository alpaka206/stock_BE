# Stock BE

미국 주식과 한국 주식을 함께 다루는 주식 리서치 서비스의 메인 백엔드입니다.

프런트엔드는 외부 시세·뉴스 API를 직접 호출하지 않고 이 백엔드의 저장된 화면 API를 호출합니다. 백엔드는 가격, 뉴스, 공시, 리서치 기록, 미디어 자료, 리포트 예약, 구독 플랜 같은 서버에서 관리해야 하는 값을 저장하고 정규화합니다.

## 기술 스택

- Spring Boot 3.5
- Java 17
- PostgreSQL
- Flyway
- Spring Data JPA
- Redis 준비
- Spring Security, OAuth2 Client, Mail 준비

Next.js는 화면과 BFF 역할에 집중합니다. Python/FastAPI는 메인 백엔드가 아니라, 필요할 때 데이터 수집·LLM 요약·Perso 더빙/자막 같은 worker로 분리합니다.

## 주요 API

화면 API:

- `GET /overview`
- `GET /radar`
- `GET /stocks/{symbol}`
- `GET /history?symbol=NVDA&range=3m`
- `GET /news`
- `GET /calendar`

저장 API:

- `GET /instruments/search?q=...`
- `POST /instruments`
- `GET /prices/bars?symbol=...`
- `POST /prices/bars`
- `GET /materials`
- `POST /materials`
- `GET /snapshots`
- `POST /snapshots`
- `DELETE /snapshots/{id}`
- `GET /subscription-plans`
- `GET /report-schedules?userId=...`
- `POST /report-schedules`
- `GET /media-assets`
- `POST /media-assets`
- `GET /localization-jobs`
- `POST /localization-jobs`
- `POST /automation/webhooks/{source}`

모든 API는 `/api/v1/*` prefix도 함께 지원합니다.

## 로컬 실행

```powershell
docker compose up --build
```

또는:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

## 데이터 저장 방향

- `instruments`: 종목 기본 정보
- `price_bars`: 일별 가격과 거래량
- `source_materials`: 뉴스, 공시, 경제 지표, 실적, 연준 발표 같은 원천 자료
- `research_snapshots`: 사용자가 저장한 판단 기록
- `report_schedules`: 오늘/이번 주 리포트 발송 예약
- `subscription_plans`: 구독별 기능 제한 정의
- `media_assets`: 어닝콜, 연준 발표, 실적 발표 오디오/영상
- `localization_jobs`: Perso 더빙/자막 작업 상태
- `automation_events`: n8n 등 외부 자동화 이벤트 수신 기록

## 환경 변수

실제 값은 커밋하지 않습니다. 필요한 항목은 `.env.example`을 기준으로 설정합니다.
