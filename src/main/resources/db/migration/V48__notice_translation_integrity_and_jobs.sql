ALTER TABLE office_notice_translation ADD COLUMN integrity_verified boolean NOT NULL DEFAULT false;

CREATE TABLE notice_translation_job (
    notice_id bigint NOT NULL REFERENCES office_notice(id) ON DELETE CASCADE,
    source_version integer NOT NULL,
    lang varchar(5) NOT NULL,
    state varchar(16) NOT NULL DEFAULT 'PENDING',
    owner uuid,
    lease_until timestamptz,
    available_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    attempts integer NOT NULL DEFAULT 0,
    PRIMARY KEY (notice_id, source_version, lang),
    CHECK (state IN ('PENDING', 'RUNNING', 'DONE', 'REJECTED', 'FAILED', 'CANCELLED'))
);
CREATE INDEX ix_notice_translation_due ON notice_translation_job(available_at) WHERE state IN ('PENDING','RUNNING');

-- Revalidate legacy machine translations through the same bounded queue; reviewed translations remain usable.
INSERT INTO notice_translation_job(notice_id, source_version, lang)
SELECT n.id, n.version, l.lang FROM office_notice n
CROSS JOIN (VALUES ('en'),('ja'),('zh'),('de'),('fr'),('vi'),('ko')) l(lang)
WHERE n.status='APPROVED' AND n.expires_at >= CURRENT_DATE AND l.lang <> n.source_lang
AND NOT EXISTS (SELECT 1 FROM office_notice_translation t WHERE t.notice_id=n.id AND t.lang=l.lang
                AND t.source_version=n.version AND t.status='REVIEWED');
