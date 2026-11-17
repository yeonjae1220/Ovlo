-- Conservative ceiling: one year from collection, never later than semester end + one year.
ALTER TABLE office_inquiry ADD COLUMN personal_data_expires_at TIMESTAMPTZ NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '1 year');
ALTER TABLE office_inquiry ADD COLUMN personal_data_erased_at TIMESTAMPTZ;
UPDATE office_inquiry SET personal_data_expires_at=asked_at+INTERVAL '1 year';
ALTER TABLE office_consultation ADD COLUMN personal_data_expires_at TIMESTAMPTZ NOT NULL DEFAULT (CURRENT_TIMESTAMP + INTERVAL '1 year');
ALTER TABLE office_consultation ADD COLUMN personal_data_erased_at TIMESTAMPTZ;
UPDATE office_consultation SET personal_data_expires_at=opened_at+INTERVAL '1 year';
CREATE INDEX idx_inquiry_privacy_due ON office_inquiry(personal_data_expires_at,member_id) WHERE personal_data_erased_at IS NULL;
CREATE INDEX idx_consultation_privacy_due ON office_consultation(personal_data_expires_at,member_id) WHERE personal_data_erased_at IS NULL;

-- Recover only recent current-version DONE jobs whose result was discarded before storage.
-- Keep original lifetime, paid attempts and cooldown. Never reopen rejected or exhausted jobs.
UPDATE notice_translation_job j SET state='PENDING'
WHERE state='DONE' AND attempts<3 AND created_at>CURRENT_TIMESTAMP-INTERVAL '48 hours'
AND EXISTS(SELECT 1 FROM office_notice n WHERE n.id=j.notice_id AND n.version=j.source_version
    AND n.status='APPROVED' AND n.expires_at>=CURRENT_DATE
    AND NOT EXISTS(SELECT 1 FROM office_notice_translation t WHERE t.notice_id=n.id AND t.lang=j.lang
        AND t.source_version=n.version AND (t.status='REVIEWED' OR t.integrity_verified)));
