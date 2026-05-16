# 백엔드 아키텍처

## 결론

메인 백엔드는 Spring Boot로 운영합니다. Next.js는 화면과 가벼운 BFF 역할에 집중하고, Python은 필요할 때 데이터 수집, LLM 요약, Perso 더빙·자막 처리 worker로 분리합니다.

## 이유

- 주식 리서치 서비스는 화면보다 저장, 트랜잭션, 권한, 구독, 리포트 발송, 작업 상태 관리가 중요합니다.
- Spring Boot는 JPA 관계 모델, Flyway migration, 트랜잭션, 보안 필터, 운영 관측성을 안정적으로 다룰 수 있습니다.
- Next.js API route는 화면 가까운 조합에는 좋지만, 장기 실행 수집 작업과 정규화된 저장소의 중심으로 쓰기에는 부담이 큽니다.
- Python은 금융/AI/미디어 SDK를 붙이기 좋아서 worker로 쓰는 편이 적합합니다.

## 데이터 흐름

1. 수집기 또는 worker가 Alpha Vantage, OpenDART, SEC EDGAR, KRX/KIS, Perso, LLM provider를 호출합니다.
2. 백엔드는 응답을 `source_materials`, `price_bars`, `media_assets`, `localization_jobs`에 저장합니다.
3. 백엔드는 저장된 자료로 `/overview`, `/radar`, `/stocks/{symbol}`, `/history`, `/news`, `/calendar` 화면 API를 구성합니다.
4. 프런트엔드는 백엔드 화면 API만 호출합니다.

## 저장 우선순위

- 1순위: 종목, 가격, 뉴스, 공시, 이벤트, 사용자 판단 기록
- 2순위: 실적 발표, 연준 발표, 어닝콜 오디오/영상 자료
- 3순위: 구독별 기능 제한, 리포트 예약, 이메일 발송 이력
- 4순위: LLM 요약 결과와 원문 출처 연결

## 원자성 및 정규화

- 저장 API는 `@Transactional`을 기본으로 사용합니다.
- 종목 삭제 시 가격과 판단 기록은 cascade 삭제합니다.
- 원천 자료는 종목 삭제 시 감사 추적을 위해 `instrument_id`만 `null` 처리합니다.
- provider/sourceKey는 중복 저장을 막기 위해 unique constraint를 둡니다.
- 가격은 instrument/date/provider 단위로 unique constraint를 둡니다.

## 화면 계약

백엔드는 프런트 화면이 바로 사용할 수 있는 화면 단위 응답을 제공합니다. 아직 저장 데이터가 부족한 경우 값을 꾸며내지 않고 `missingData`와 낮은 `confidence`로 명시합니다.

## 로그인과 토큰 관리

- Google OAuth 성공 시 백엔드가 사용자 계정을 upsert합니다.
- access token은 HMAC 서명 JWT로 만들고 `HttpOnly` cookie에만 저장합니다.
- refresh token은 무작위 opaque token으로 만들고 원문은 사용자 브라우저 cookie에만 둡니다.
- DB에는 refresh token의 SHA-256 hash, 만료 시각, 폐기 시각, user agent, IP를 저장합니다.
- refresh 요청은 기존 refresh token을 폐기하고 새 refresh token을 발급합니다.
- 프런트는 token을 localStorage에 저장하지 않고, Next.js route handler나 브라우저 fetch의 cookie 흐름을 사용합니다.
- CSRF는 유지합니다. 프런트가 mutating 요청을 보낼 때는 `/csrf`로 token을 받은 뒤 header에 싣습니다.

## Swagger/OpenAPI

- Swagger UI: `/swagger-ui.html`
- OpenAPI JSON: `/v3/api-docs`
- API는 화면, 저장, 플랫폼, 인증 영역별 tag를 갖습니다.
- OpenAPI components에는 access/refresh cookie 보안 스키마가 포함됩니다.
