# 데이터 provider 계획

## 1차 provider

- Alpha Vantage: 미국장 가격, 뉴스, 경제 지표, 실적 캘린더
- SEC EDGAR: 미국 공시
- OpenDART: 국내 공시
- KRX/KIS 또는 공공데이터포털: 국내장 가격과 종목 정보

## 미디어 provider

- Perso: 어닝콜, 연준 발표, 실적 발표 영상의 더빙·자막 작업
- 백엔드는 `media_assets`와 `localization_jobs`에 원본과 작업 상태를 저장한다.
- 실제 provider 호출은 키와 계약이 준비된 뒤 worker 또는 adapter로 붙인다.

## LLM provider

- LLM은 수치를 만들지 않는다.
- 저장된 원천 자료를 요약하고, 응답에는 항상 source material id를 연결한다.
- OpenAI, Gemini 등은 provider adapter로 교체 가능하게 둔다.

## 프런트 계약

- 프런트는 외부 API 키를 알 필요가 없다.
- 프런트는 저장된 화면 API와 source ref만 읽는다.
- 목데이터를 쓰는 경우 백엔드가 명시적으로 mock/fallback 상태를 내려야 한다.
