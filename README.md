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

문서는 로컬 실행 후 `http://localhost:8080/swagger-ui.html`에서 확인합니다. 원본 OpenAPI JSON은 `GET /v3/api-docs`입니다.

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
- `GET /reports?userId=...`
- `POST /reports/preview`
- `POST /reports/send`
- `GET /media-assets`
- `POST /media-assets`
- `GET /localization-jobs`
- `POST /localization-jobs`
- `POST /localization-jobs/{jobId}/submit`
- `POST /localization-jobs/{jobId}/sync`
- `POST /provider-ingest/alpha-vantage/daily`
- `POST /provider-ingest/alpha-vantage/news`
- `POST /provider-ingest/opendart/disclosures`
- `POST /provider-ingest/sec/submissions`
- `POST /automation/webhooks/{source}`
- `GET /csrf`
- `POST /auth/dev-login`
- `POST /auth/refresh`
- `POST /auth/logout`
- `GET /auth/me`

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
- `report_deliveries`: 리포트 생성, SMTP 발송, 실패 이력
- `subscription_plans`: 구독별 기능 제한 정의
- `media_assets`: 어닝콜, 연준 발표, 실적 발표 오디오/영상
- `localization_jobs`: Perso 더빙/자막 작업 상태
- `automation_events`: n8n 등 외부 자동화 이벤트 수신 기록
- `user_accounts`: Google OAuth 또는 개발용 로그인으로 생성된 사용자 계정
- `refresh_sessions`: 원문 refresh token이 아닌 해시된 refresh token 세션

## 인증 전략

- 백엔드가 `HttpOnly` cookie로 `stock_access_token`과 `stock_refresh_token`을 발급합니다.
- access token은 짧게 유지하고 기본 TTL은 15분입니다.
- refresh token은 30일 기본 TTL을 가지며, 서버 DB에는 SHA-256 hash만 저장합니다.
- `/auth/refresh` 호출 시 기존 refresh session은 폐기되고 새 refresh token으로 회전합니다.
- `/auth/logout`은 현재 refresh session을 폐기하고 두 cookie를 모두 삭제합니다.
- 프런트는 token 원문을 JavaScript 상태나 localStorage에 저장하지 않습니다. Next.js route handler가 필요할 때 `/csrf`를 먼저 호출하고, cookie 기반으로 백엔드에 요청합니다.
- Google OAuth는 `/oauth2/authorization/google` 진입 후 성공 시 백엔드가 cookie를 설정하고 `AUTH_FRONTEND_CALLBACK_URL`로 redirect하는 구조입니다.
- 실제 접근 제한은 테스트 편의를 위해 아직 강제하지 않았지만, Swagger에는 cookie 보안 스키마가 문서화되어 있습니다.

## 환경 변수

실제 값은 커밋하지 않습니다. 필요한 항목은 `.env.example`을 기준으로 설정합니다.

주요 provider 키:

- `ALPHA_VANTAGE_API_KEY`: 미국장 일봉과 뉴스 저장
- `OPENDART_API_KEY`: 국내 공시 저장
- `SEC_USER_AGENT`: SEC EDGAR API 호출용 식별자
- `PERSO_API_KEY`, `PERSO_SPACE_SEQ`: Perso 더빙/자막 작업 제출
- `REPORT_EMAIL_SENDING_ENABLED`, `SMTP_HOST`, `SMTP_PORT`, `REPORT_FROM_EMAIL`: 리포트 이메일 발송

키가 없으면 백엔드는 목데이터를 만들지 않고 `428 Precondition Required`로 설정 누락을 알려줍니다.
