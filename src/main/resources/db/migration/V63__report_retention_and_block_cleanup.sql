-- 신고 기록을 처리 후 1년이 지나면 익명화한다(개인정보처리방침 v4). 행은 통계용으로 남기고
-- 신고자·대상·자유 기재 사유처럼 사람을 알아볼 수 있는 값만 지운다. 익명화된 행은 anonymized_at 이 찍힌다.
ALTER TABLE content_report ALTER COLUMN target_id DROP NOT NULL;
ALTER TABLE content_report ALTER COLUMN target_author_id DROP NOT NULL;
ALTER TABLE content_report ALTER COLUMN reporter_id DROP NOT NULL;
ALTER TABLE content_report ADD COLUMN anonymized_at TIMESTAMPTZ;
CREATE INDEX idx_content_report_retention ON content_report (resolved_at) WHERE anonymized_at IS NULL AND resolved_at IS NOT NULL;

-- 중고거래 신고는 처리 상태가 없어 접수 시각을 기준으로 한다. listing_id 도 판매자를 가리키므로 함께 지운다.
ALTER TABLE marketplace_report ALTER COLUMN listing_id DROP NOT NULL;
ALTER TABLE marketplace_report ALTER COLUMN reporter_id DROP NOT NULL;
ALTER TABLE marketplace_report ALTER COLUMN reason DROP NOT NULL;
ALTER TABLE marketplace_report ADD COLUMN anonymized_at TIMESTAMPTZ;
CREATE INDEX idx_marketplace_report_retention ON marketplace_report (created_at) WHERE anonymized_at IS NULL;

-- 탈퇴한 회원은 복원되지 않으므로 그 회원이 걸었거나 당한 차단은 쓸모가 없다. 이후 탈퇴는 탈퇴 처리에서 지운다.
DELETE FROM member_block b
WHERE EXISTS (SELECT 1 FROM member m WHERE m.status = 'WITHDRAWN' AND m.id IN (b.actor_id, b.target_id));
