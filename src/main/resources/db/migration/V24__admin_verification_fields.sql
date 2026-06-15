-- V24: 관리자 수동 인증(ADMIN_VERIFIED) 지원
-- ============================================================================
-- 1) 수동 인증은 학교 이메일이 없을 수 있으므로 verified_email NULL 허용.
--    활성 이메일 UNIQUE 인덱스(uq_vc_verified_email_active)는 식 lower(verified_email)
--    기반인데, lower(NULL)=NULL 이고 PostgreSQL B-tree는 식 결과가 NULL인 행을
--    인덱스에 넣지 않으므로 이메일 없는 수동 자격이 여러 건이어도 유일성 제약에
--    걸리지 않는다. (NULL을 'distinct'로 비교해서가 아니라 인덱스 대상에서 제외되기 때문.)
-- 2) 감사용 컬럼: 발급 관리자(verified_by), 사유(note).
-- ============================================================================

ALTER TABLE verification_credential ALTER COLUMN verified_email DROP NOT NULL;

ALTER TABLE verification_credential ADD COLUMN verified_by VARCHAR(255);
ALTER TABLE verification_credential ADD COLUMN note        VARCHAR(500);
