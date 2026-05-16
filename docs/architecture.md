# 백엔드 아키텍처

## 결론

메인 백엔드는 Spring Boot로 운영한다. Next.js는 프런트와 얇은 BFF 역할을 맡고, FastAPI/Python은 필요할 때 데이터 수집, LLM 요약, Perso 더빙·자막 작업을 처리하는 worker로 분리한다.

## 이유

- 주식 리서치 서비스는 읽기 전용 화면처럼 보이지만 실제로는 저장, 트랜잭션, 권한, 구독, 결제, 리포트 발송, 작업 상태 관리가 중심이다.
- Spring Boot는 DB 트랜잭션, JPA 관계 모델, Flyway migration, 보안 필터, 관측성 구성이 강하다.
- Next.js API route는 화면 가까운 조합에는 좋지만, 장기 실행 수집 작업과 정규화된 데이터 저장소의 중심으로 두기에는 약하다.
- Python은 금융/AI/미디어 SDK를 다루기 좋으므로 worker로 붙이는 편이 좋다.

## 데이터 흐름

1. 수집기 또는 worker가 Alpha Vantage, OpenDART, SEC EDGAR, KRX/KIS, Perso, LLM provider를 호출한다.
2. 백엔드가 응답을 `source_materials`, `price_bars`, `media_assets`, `localization_jobs`에 저장한다.
3. 백엔드는 저장된 자료를 기준으로 화면 API를 구성한다.
4. 프런트는 백엔드 API만 호출한다.

## 저장 우선순위

- 1순위: 종목, 가격, 뉴스, 공시, 이벤트, 사용자 판단 기록
- 2순위: 실적 발표, 연준 발표, 어닝콜 오디오·영상 자료
- 3순위: 구독제 기능 제한, 리포트 예약, 이메일 발송 이력
- 4순위: LLM 요약 결과와 원문 출처 연결

## 원자성

- 저장 API는 `@Transactional`을 기본으로 사용한다.
- 종목 삭제 시 가격 바와 판단 기록은 cascade 삭제한다.
- 원천 자료는 종목이 삭제되어도 감사 추적을 위해 `instrument_id`만 null 처리한다.
- provider/sourceKey는 중복 저장을 막기 위해 unique constraint를 둔다.
