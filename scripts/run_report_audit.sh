#!/usr/bin/env bash
# ============================================================================
# 대학 리포트 콘텐츠 품질 감사 실행기 (READ-ONLY)
#   audit_report_quality.sql 을 k8s 운영 postgres 파드에 흘려 결함을 집계한다.
#   순수 SELECT + TEMP VIEW 만 사용 → 실데이터 변경 없음.
#
# 사용법:
#   scripts/run_report_audit.sh              # 요약+상세 전체 출력
#   scripts/run_report_audit.sh > audit.txt  # 파일로 저장
#
# 전제: `ssh lenovo` 로 k3s 접근 가능, ovlo 네임스페이스 postgres-0 파드.
# ============================================================================
set -euo pipefail

SSH_HOST="${OVLO_DB_SSH:-lenovo}"
NS="${OVLO_NS:-ovlo}"
POD="${OVLO_PG_POD:-postgres-0}"
DB="${OVLO_DB:-ovlo}"
USER_="${OVLO_DB_USER:-ovlo}"
SQL_FILE="$(dirname "$0")/audit_report_quality.sql"

[[ -f "$SQL_FILE" ]] || { echo "SQL not found: $SQL_FILE" >&2; exit 1; }

ssh "$SSH_HOST" "sudo kubectl exec -i -n '$NS' '$POD' -- psql -U '$USER_' -d '$DB' -v ON_ERROR_STOP=1" < "$SQL_FILE"
