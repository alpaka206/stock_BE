# 데이터 Provider 계획

## 1차 Provider

- Alpha Vantage: 미국장 가격, 뉴스, 경제 지표, 실적 캘린더
- SEC EDGAR: 미국 공시
- OpenDART: 국내 공시
- KRX/KIS 또는 공공데이터포털: 국내장 가격과 종목 정보

## 미디어 Provider

- Perso: 어닝콜, 연준 발표, 실적 발표 영상의 더빙·자막 작업
- 백엔드는 `media_assets`와 `localization_jobs`에 원본과 작업 상태를 저장합니다.
- 실제 provider 호출은 계정과 API key가 준비된 뒤 worker 또는 adapter로 연결합니다.

## LLM Provider

- LLM은 가격이나 사실을 만들지 않습니다.
- 저장된 원천 자료를 요약하고, 응답에는 항상 source material id를 연결합니다.
- OpenAI, Gemini 등은 provider adapter로 교체 가능하게 둡니다.

## 프런트 계약

- 프런트엔드는 외부 API 키를 들고 있지 않습니다.
- 프런트엔드는 백엔드의 저장된 화면 API와 source ref만 사용합니다.
- 목데이터가 표시되는 경우 UI에서 `(목데이터)`로 명확히 알립니다.
