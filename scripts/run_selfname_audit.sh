#!/usr/bin/env bash
# ============================================================================
# 전 언어 자기명(self-name) 크로스언어 감사 실행기 (READ-ONLY)
#   audit_selfname_xlang.sql 을 k8s 운영 postgres 파드에 흘려
#   "body가 다른 대학을 자기 학교로 지칭하는" 결함을 자동 triage 한다.
#   순수 SELECT + TEMP VIEW/FUNCTION 만 사용 → 실데이터 변경 없음.
#
# 사용법:
#   scripts/run_selfname_audit.sh                 # 전체 출력
#   scripts/run_selfname_audit.sh > audit.txt     # 파일로 저장
#   scripts/run_selfname_audit.sh | sed -n '/SWAP/,/UNRESOLVED/p'   # 확정분만
#
# 출력 섹션:
#   SWAP        — body 자기명이 '다른 실존 대학'의 검증명과 일치(고신뢰, 교정 대상)
#   UNRESOLVED  — 자기 정본명 부재 + 타교 매칭도 없음(음차변이/파손, 수동 판단)
#   en anchor-missing — 참고용(대부분 약자/악센트 오탐)
#   SUMMARY     — 카운트
#
# ⚠️ 교정 원칙(GLOBAL-PIT-063): SWAP도 그대로 믿지 말고 반드시
#    ① en/ko 등 다른 언어 body와 크로스 대조로 정답 방향 확정
#    ② 치환은 '대학명 전체 구문' 리터럴로(도시/지명 단독 치환 금지)
#    ③ 백업 테이블 생성 + dry-run(SELECT) 후 UPDATE, 직후 잔존 0 재검증
#
# 전제: `ssh lenovo` 로 k3s 접근 가능, ovlo 네임스페이스 postgres-0 파드.
# ============================================================================
set -euo pipefail

SSH_HOST="${OVLO_DB_SSH:-lenovo}"
NS="${OVLO_NS:-ovlo}"
POD="${OVLO_PG_POD:-postgres-0}"
DB="${OVLO_DB:-ovlo}"
USER_="${OVLO_DB_USER:-ovlo}"
SQL_FILE="$(dirname "$0")/audit_selfname_xlang.sql"

[[ -f "$SQL_FILE" ]] || { echo "SQL not found: $SQL_FILE" >&2; exit 1; }

ssh "$SSH_HOST" "sudo kubectl exec -i -n '$NS' '$POD' -- psql -U '$USER_' -d '$DB' -P pager=off" < "$SQL_FILE"
