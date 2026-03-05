-- MediaFile 테이블
CREATE TABLE media_file (
    id                  BIGSERIAL       PRIMARY KEY,
    uploader_id         BIGINT          NOT NULL,
    media_type          VARCHAR(30)     NOT NULL,
    storage_path        TEXT            NOT NULL,
    storage_type        VARCHAR(10)     NOT NULL DEFAULT 'LOCAL',
    original_filename   TEXT            NOT NULL,
    file_size           BIGINT          NOT NULL,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

-- 업로더 기준 조회 (내가 올린 미디어 목록)
CREATE INDEX idx_media_file_uploader_id ON media_file (uploader_id);
