# 인증 전략

## 목표

로그인은 Google OAuth를 기본 경로로 둡니다. 프런트는 token 원문을 직접 관리하지 않고, 백엔드가 발급한 `HttpOnly` cookie만 사용합니다.

## Cookie

- `stock_access_token`: 짧은 수명의 access token
- `stock_refresh_token`: 긴 수명의 refresh token
- 둘 다 `HttpOnly`, `SameSite=Lax`, `Path=/`로 발급합니다.
- 운영에서는 `AUTH_COOKIE_SECURE=true`와 적절한 `AUTH_COOKIE_DOMAIN`을 설정합니다.

## Access Token

- HMAC-SHA256 서명 JWT
- 기본 TTL: 15분
- payload: issuer, user id, email, role, issued at, expiry
- 프런트 JavaScript에서는 읽지 않습니다.

## Refresh Token

- 랜덤 opaque token
- 기본 TTL: 30일
- DB에는 원문을 저장하지 않고 SHA-256 hash만 저장합니다.
- `/auth/refresh` 호출 시 기존 세션을 폐기하고 새 refresh token을 발급합니다.
- `/auth/logout`은 현재 refresh session을 폐기하고 cookie를 삭제합니다.

## 프런트 연동

1. 사용자가 로그인 버튼을 누르면 백엔드 `/oauth2/authorization/google`로 이동합니다.
2. Google OAuth 성공 후 백엔드가 cookie를 설정하고 `AUTH_FRONTEND_CALLBACK_URL`로 redirect합니다.
3. 프런트는 `/auth/me` 또는 프런트 BFF route를 통해 사용자 상태를 확인합니다.
4. access token이 만료되면 프런트 BFF가 `/csrf`를 호출한 뒤 `/auth/refresh`로 세션을 회전합니다.
5. 로그아웃은 `/auth/logout`으로 요청하고, 백엔드가 cookie를 삭제합니다.

## 현재 상태

접근 제한은 아직 강제하지 않습니다. 테스트와 화면 개발 편의를 위해 API는 열어두되, 인증 세션과 Swagger 문서는 실서비스 구조에 맞춰 준비해 둡니다.
