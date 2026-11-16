ALTER TABLE notice_translation_job DROP CONSTRAINT notice_translation_job_state_check;
ALTER TABLE notice_translation_job ADD CONSTRAINT notice_translation_job_state_check
    CHECK (state IN ('PENDING','RUNNING','PAUSED','DONE','REJECTED','FAILED','CANCELLED'));

-- Recover only still-eligible, unexhausted cancellations from the old suspension path.
-- Never reset paid attempts, the 48-hour lifetime, or available_at cooldown.
UPDATE notice_translation_job j SET state='PAUSED'
WHERE state='CANCELLED' AND attempts<3 AND created_at>CURRENT_TIMESTAMP-INTERVAL '48 hours'
AND EXISTS(SELECT 1 FROM office_notice n WHERE n.id=j.notice_id AND n.version=j.source_version
    AND n.status='APPROVED' AND n.expires_at>=CURRENT_DATE);
