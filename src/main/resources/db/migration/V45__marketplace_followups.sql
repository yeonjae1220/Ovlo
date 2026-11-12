-- Short-lived cross-instance claims; no transaction stays open during a provider call.
CREATE TABLE marketplace_translation_lease (
    listing_id BIGINT NOT NULL REFERENCES marketplace_listing(id),
    version BIGINT NOT NULL,
    language VARCHAR(2) NOT NULL,
    owner UUID NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY(listing_id, version, language)
);

ALTER TABLE marketplace_photo ADD COLUMN orphaned_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE marketplace_photo ADD COLUMN deletion_pending BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE marketplace_photo ADD COLUMN cleanup_retry_at TIMESTAMPTZ;
-- Existing unused uploads receive a fresh grace period at migration time.
UPDATE marketplace_photo p SET orphaned_at=NULL
WHERE EXISTS(SELECT 1 FROM marketplace_listing_photo lp WHERE lp.photo_id=p.id);
CREATE INDEX idx_marketplace_photo_cleanup ON marketplace_photo(orphaned_at,cleanup_retry_at)
WHERE orphaned_at IS NOT NULL OR deletion_pending;
