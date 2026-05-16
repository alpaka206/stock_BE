# Stock BE

미국 주식과 한국 주식을 함께 다루는 주식 리서치 서비스의 메인 백엔드다.

프런트가 외부 API를 직접 호출하지 않도록, 백엔드가 자료를 수집·정규화·저장하고 프런트는 저장된 API만 읽는 구조를 기준으로 한다.

## 기술 스택

- Spring Boot 3
- Java 17
- Postgres
- Flyway
- Spring Data JPA
- Redis 확장 준비
- OAuth2, Mail, 결제 연동 확장 준비

Next.js는 화면과 BFF 역할로 유지한다. Perso, LLM, 대용량 수집, 영상 처리 같은 작업은 Spring이 작업 상태를 관리하고 필요하면 Python worker가 처리하는 구조로 확장한다.

## 핵심 API

- `GET /health`
- `GET /readyz`
- `GET /overview`
- `GET /instruments/search?q=...`
- `POST /instruments`
- `GET /materials`
- `POST /materials`
- `GET /snapshots`
- `POST /snapshots`
- `DELETE /snapshots/{id}`

`/api/v1/*` prefix도 함께 지원한다.

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
- `price_bars`: 일별 가격 바
- `source_materials`: 뉴스, 공시, 경제 지표, 실적, 연준 자료, 원문 payload
- `research_snapshots`: 사용자 판단 기록
- `report_schedules`: 오늘/이번주 리포트 발송 예약
- `subscription_plans`: 구독제 기능 제한 정의
- `media_assets`: 어닝콜, 연준 발표, 영상·오디오 자료
- `localization_jobs`: Perso 등 더빙·자막 작업 상태

## 환경변수

실제 값은 커밋하지 않는다. 필요한 항목은 `.env.example`을 기준으로 설정한다.
