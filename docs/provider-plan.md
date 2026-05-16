# 데이터 Provider 계획

## 1차 Provider

- Alpha Vantage: 미국장 가격, 뉴스, 경제 지표, 실적 캘린더
- SEC EDGAR: 미국 공시
- OpenDART: 국내 공시
- KRX/KIS 또는 공공데이터포털: 국내장 가격과 종목 정보

## 현재 저장형 ingest API

- `POST /provider-ingest/alpha-vantage/daily`
  - Alpha Vantage `TIME_SERIES_DAILY` 응답을 `price_bars`에 저장합니다.
  - 종목이 없으면 미국장 기본 종목으로 먼저 등록합니다.
- `POST /provider-ingest/alpha-vantage/news`
  - Alpha Vantage `NEWS_SENTIMENT` feed를 `source_materials`의 `NEWS`로 저장합니다.
- `POST /provider-ingest/opendart/disclosures`
  - OpenDART `list.json` 공시 목록을 `source_materials`의 `DISCLOSURE`로 저장합니다.
  - `corpCode`는 필수입니다. 종목 연결이 필요하면 `symbol`도 함께 보냅니다.
- `POST /provider-ingest/sec/submissions`
  - SEC `data.sec.gov/submissions/CIK##########.json`의 최근 filing을 `DISCLOSURE`로 저장합니다.

API key가 없거나 provider가 오류를 반환하면 실제 자료를 만들지 않고 실패 응답을 반환합니다.

## 미디어 Provider

- Perso: 어닝콜, 연준 발표, 실적 발표 영상의 더빙·자막 작업
- 백엔드는 `media_assets`와 `localization_jobs`에 원본과 작업 상태를 저장합니다.
- `POST /localization-jobs/{jobId}/submit`은 Perso 외부 영상 업로드 또는 오디오 등록 후 번역 작업을 제출하고 `provider_job_id`를 저장합니다.
- `POST /localization-jobs/{jobId}/sync`는 Perso 진행 상태와 결과 링크를 조회해 작업 상태를 갱신합니다.
- 장시간 polling은 HTTP 요청 안에서 오래 기다리지 않고 별도 worker나 n8n에서 주기적으로 sync를 호출하는 방식으로 확장합니다.

## LLM Provider

- LLM은 가격이나 사실을 만들지 않습니다.
- 저장된 원천 자료를 요약하고, 응답에는 항상 source material id를 연결합니다.
- OpenAI, Gemini 등은 provider adapter로 교체 가능하게 둡니다.

## 리포트와 이메일

- `POST /reports/preview`: 저장된 가격, 뉴스, 공시 자료로 이메일 본문을 생성합니다.
- `POST /reports/send`: 발송 이력을 `report_deliveries`에 저장합니다.
- `REPORT_EMAIL_SENDING_ENABLED=false`이면 SMTP로 보내지 않고 `READY` 상태로 저장합니다.
- 운영에서 SMTP를 켜면 성공은 `SENT`, 실패는 `FAILED`와 오류 메시지로 남깁니다.

## 프런트 계약

- 프런트엔드는 외부 API 키를 들고 있지 않습니다.
- 프런트엔드는 백엔드의 저장된 화면 API와 source ref만 사용합니다.
- 목데이터가 표시되는 경우 UI에서 `(목데이터)`로 명확히 알립니다.
