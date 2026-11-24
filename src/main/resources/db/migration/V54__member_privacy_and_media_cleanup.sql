-- Existing withdrawn accounts are intentionally NOT bulk rewritten by migration.
-- Run the reviewed preflight/backfill procedure after a verified backup.
ALTER TABLE member ADD COLUMN personal_data_erased_at TIMESTAMPTZ;
CREATE INDEX idx_member_privacy_pending ON member(id)
    WHERE status='WITHDRAWN' AND personal_data_erased_at IS NULL;

CREATE TABLE media_deletion_job (
    media_id BIGINT PRIMARY KEY REFERENCES media_file(id) ON DELETE CASCADE,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    attempts INT NOT NULL DEFAULT 0
);

-- Serialize private child writes with withdrawal. A request authenticated before
-- withdrawal must not recreate credentials, personal events or uploads afterward.
CREATE FUNCTION require_current_member_for_private_write() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE current_state varchar;
DECLARE owner_id bigint;
BEGIN
    IF TG_TABLE_NAME='media_file' THEN
        owner_id := NEW.uploader_id;
    ELSE
        owner_id := NEW.member_id;
    END IF;
    SELECT status INTO current_state FROM member WHERE id=owner_id FOR UPDATE;
    IF current_state IS NULL OR current_state='WITHDRAWN' THEN
        RAISE EXCEPTION 'Member no longer accepts private writes' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER verification_active_owner BEFORE INSERT OR UPDATE ON verification_credential
    FOR EACH ROW EXECUTE FUNCTION require_current_member_for_private_write();
CREATE TRIGGER personal_event_active_owner BEFORE INSERT OR UPDATE ON personal_event
    FOR EACH ROW EXECUTE FUNCTION require_current_member_for_private_write();
CREATE TRIGGER media_active_owner BEFORE INSERT ON media_file
    FOR EACH ROW EXECUTE FUNCTION require_current_member_for_private_write();
