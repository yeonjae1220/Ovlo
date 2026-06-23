# Security Policy

Ovlo(교환학생 커뮤니티 플랫폼)의 보안 정책과 취약점 신고 절차입니다.

## 지원 범위

| 영역 | 위치 |
|------|------|
| 백엔드 보안 아키텍처 | [docs/backend-security.md](docs/backend-security.md) |
| 프론트엔드 보안(CSP, 토큰 처리) | [docs/frontend-security.md](docs/frontend-security.md) |
| 인증/토큰 설계 결정 | [docs/adr/0003-refresh-token-hashing.md](docs/adr/0003-refresh-token-hashing.md) |

## 취약점 신고

보안 취약점을 발견하면 **공개 이슈로 등록하지 말고** 비공개로 신고해 주세요.

- 연락: 저장소 관리자(GitHub Security Advisory의 "Report a vulnerability") 또는
  메인테이너 이메일.
- 포함 정보: 영향 받는 컴포넌트, 재현 절차, 예상 영향도, 가능하면 PoC.
- 응답 목표: 영업일 기준 72시간 내 1차 회신.

조정된 공개(coordinated disclosure)를 원칙으로 하며, 패치 배포 전까지 상세 공개를
보류해 주시기를 요청합니다.

## 보안 원칙 (요약)

이 프로젝트가 강제하는 핵심 통제:

- **비밀정보 비하드코딩**: API 키·비밀번호·토큰은 환경변수/시크릿 매니저로만 주입.
  `.env*`는 `.gitignore` 처리.
- **Refresh Token 해시**: Redis 저장 전 SHA-256 해시 (평문 저장 금지, GLOBAL-PIT-001).
  → [ADR-0003](docs/adr/0003-refresh-token-hashing.md)
- **입력 검증**: 시스템 경계(`adapter/in/web`)에서 `@Valid`로 검증, 도메인 불변식으로 재확인.
- **인증/인가**: JWT(Access 15분 stateless) + Redis 세션. 보호 자원은 유효 토큰 필수.
- **CSP**: 프론트는 요청마다 nonce 생성(`script-src 'nonce-{n}' 'strict-dynamic'`).
- **CSRF/Rate limit**: 상태 변경 엔드포인트 보호.

## 의존성 보안

- 의존성 업데이트와 알려진 취약점(CVE) 모니터링을 정기적으로 수행합니다.
- 빌드: Gradle(`./gradlew`), 프론트: npm. 락파일을 통한 재현 가능 빌드를 유지합니다.

## 회귀 방지

확인된 취약점은 재발 방지를 위해 회귀 테스트와 전역 Pitfall 추적으로 관리합니다
(예: `RedisTokenAdapterTest`, `TokenHashUtilTest`).
