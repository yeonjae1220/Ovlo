-- V25: 관리자 인증 취소 감사 컬럼
-- ============================================================================
-- 관리자가 자격을 취소(EXPIRED)할 때 취소자/취소시각을 보존한다.
-- 시스템 자동 만료(만료 스케줄러)는 두 컬럼을 채우지 않아 관리자 취소와 구분된다.
-- ============================================================================

ALTER TABLE verification_credential ADD COLUMN revoked_by VARCHAR(255);
ALTER TABLE verification_credential ADD COLUMN revoked_at TIMESTAMPTZ;
