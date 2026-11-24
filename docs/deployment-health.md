# 배포 교체와 HTTP 헬스체크

## 확인한 문제

2026-11-24 12:50 KST 배포에서 홈 요청 1회가 15초 제한을 넘겼다. 회전된
ingress-nginx 로그를 복구해 보니 해당 요청은 새 파드가 아닌 이전 frontend
`10.42.0.127:3000`을 대상으로 14.345초 기다리다가 499로 끝났다(03:51:09 UTC 로그).
이후 새 파드 `10.42.0.45:3000`의 첫 확인 요청은 200, upstream 0.248초였다.
같은 이미지·CPU/메모리 한도의 격리된 임시 파드에서도 첫 홈 응답은 약 0.26초였다.
즉 관측된 지연은 종료되는 이전 upstream으로 향한 요청에서 발생했으며,
홈 페이지의 콜드 스타트가 15초 걸린 증거는 없었다.

당시 종료 전 대기 훅은 없었고 ingress 연결 제한은 60초였다. Kubernetes의 파드 종료와
EndpointSlice 갱신은 함께 진행되므로 앱을 즉시 종료하면 ingress에 남은 목적지로
향하는 요청이 끊길 수 있다. [Kubernetes 종료 절차](https://kubernetes.io/docs/concepts/workloads/pods/pod-termination/)

백엔드는 별개의 설정 불일치가 있었다. 보안 필터가 기본 `/actuator/health`만 허용해
readiness/liveness URL은 401이었고, CI가 `set image`만 수행해 저장소에 있던 HTTP
프로브가 운영에 반영되지 않았다. 실제 운영은 TCP readiness, liveness 없음이었다.

## 변경과 경계

- 두 Deployment는 `maxUnavailable: 0`, `maxSurge: 1`, `minReadySeconds: 10`을 명시한다.
- 두 컨테이너는 SIGTERM 전에 `sleep 15`로 ingress 갱신과 기존 요청 완료 시간을 확보한다.
  종료 유예는 backend 60초, frontend 45초다. Spring의 요청 종료 대기는 30초다.
- ingress 연결 제한은 3초, 다음 upstream 시도는 `error timeout`, 총 시도 상한은 2회다.
  재시도 시간 제한은 5초다. `non_idempotent`를 켜지 않으므로 이미 전송한 POST 등을
  무조건 재실행하지 않는다. WebSocket용 읽기/쓰기 제한 3600초는 유지한다.
  [NGINX 재시도 규칙](https://nginx.org/en/docs/http/ngx_http_proxy_module.html#proxy_next_upstream)
- 정확한 GET `/actuator/health`, `/actuator/health/readiness`, `/actuator/health/liveness`만
  인증 없이 접근한다. 두 그룹은 인증 사용자에게도 구성요소/상세를 반환하지 않는다.
  하위 상세 경로와 metrics/prometheus 등은 계속 ADMIN 경계 안에 있다.
- backend startup/liveness는 liveness 그룹, readiness는 readiness+DB+Redis 그룹을 쓴다.
  frontend 프로브는 자체 `/health/live`만 검사한다. DB 장애를 전체 화면 503으로 전파하는
  frontend→backend readiness 의존을 만들지 않는다.
- CI는 `app.yaml`에서 Deployment만 추출해 이미지 SHA와 명시된 degraded 모드를 설정한 뒤
  같은 파일에 `kubectl diff`→`apply`를 수행한다. PVC와 Service는 이 변환에서 제외한다.
  기존 환경변수는 이름별로 대조했으며 빠지는 항목은 없다. 개인정보 안내와 AI 정책은 변경하지 않는다.
- ingress도 이미지 교체 전에 diff/apply하고, 배포가 끝나면 내부 HTTP 프로브 200/UP,
  상세 비노출, metrics 비로그인 401을 CI에서 검증한다.

## 검증과 운영 점검

`HealthProbeSecurityTest`는 기존 코드에서 두 경로의 401로 실패함을 먼저 확인했다.
수정 후 200/UP·503 상태 전환, 상세 비노출, 일반 회원의 actuator 접근 금지,
정확한 GET 경로만 공개되는지를 실제 Spring Security 필터로 검증한다.
렌더러는 kubectl의 여러 JSON 문서 출력, 비밀 참조/프로브 보존, 저장소 리소스 제외,
잘못된 이미지·대상·운영 모드 거부를 검증한다.

배포 후 실제 이미지, HTTP 프로브, 파드 재시작, endpoint 전환과 ingress 로그를 확인한다.
공개 홈/API 및 내부 ingress 요청을 교체 전부터 병렬로 측정하고 요청 ID로 로그를 대조한다.
처음 교체하는 기존 파드에는 새 preStop 훅이 없으므로, 새 파드로 전환한 뒤 동일 이미지의
추가 rolling restart도 관측해 종료 훅이 적용된 교체를 별도로 검증한다.
측정 표본은 무중단 SLA나 과부하 상태의 보장이 아니다.

## 롤백

이전 이미지 `sha-b038e84`로 **이미지만** 되돌리면 새 HTTP 프로브가 다시 401이 된다.
이전 이미지로 복구해야 한다면 해당 이미지와 호환되는 프로브/종료 설정을 한 Deployment
문서에 함께 넣고 diff/apply한다. 새 변경에 DB 마이그레이션은 없다.
2026-11-24 작업의 배포 전 Deployment/Ingress 사본은 lenovo의 접근 제한 파일
`/tmp/ovlo-rollout-candidate-20261124/before-{app,frontend,ovlo-ingress}.json`에 보관했다.
그 사본은 긴급 복구용이며 후속 배포로 구성이 달라졌다면 그대로 적용하지 않는다.
