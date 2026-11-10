-- Calendar v1 uses the canonical global_universities catalog.
CREATE TABLE university_event (
    university_id BIGINT NOT NULL REFERENCES global_universities(id),
    origin VARCHAR(30) NOT NULL CHECK (origin IN ('UNIVERSITY','OVLO_CURATED')),
    status VARCHAR(30) NOT NULL CHECK (status IN ('DRAFT','PUBLISHED','CANCELLED')),
    source_url VARCHAR(2000), source_checked_on DATE,
    created_by BIGINT NOT NULL, updated_by BIGINT NOT NULL,
    CHECK (origin <> 'OVLO_CURATED' OR (source_url IS NOT NULL AND source_checked_on IS NOT NULL)),

    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(30) NOT NULL CHECK (category IN ('ADMINISTRATION','ACADEMIC','EVENT','OTHER')),
    all_day BOOLEAN NOT NULL,
    start_date DATE, end_date DATE,
    starts_at TIMESTAMPTZ, ends_at TIMESTAMPTZ,
    time_zone VARCHAR(100) NOT NULL,
    location VARCHAR(300), action_url VARCHAR(2000), deadline_at TIMESTAMPTZ, audience VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
    CHECK ((all_day AND start_date IS NOT NULL AND end_date IS NOT NULL AND end_date > start_date AND starts_at IS NULL AND ends_at IS NULL)
        OR (NOT all_day AND starts_at IS NOT NULL AND ends_at IS NOT NULL AND ends_at > starts_at AND start_date IS NULL AND end_date IS NULL))
);
CREATE TABLE personal_event (
    member_id BIGINT NOT NULL REFERENCES member(id),

    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL DEFAULT 0,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(30) NOT NULL CHECK (category IN ('ADMINISTRATION','ACADEMIC','EVENT','OTHER')),
    all_day BOOLEAN NOT NULL,
    start_date DATE, end_date DATE,
    starts_at TIMESTAMPTZ, ends_at TIMESTAMPTZ,
    time_zone VARCHAR(100) NOT NULL,
    location VARCHAR(300), action_url VARCHAR(2000), deadline_at TIMESTAMPTZ, audience VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
    CHECK ((all_day AND start_date IS NOT NULL AND end_date IS NOT NULL AND end_date > start_date AND starts_at IS NULL AND ends_at IS NULL)
        OR (NOT all_day AND starts_at IS NOT NULL AND ends_at IS NOT NULL AND ends_at > starts_at AND start_date IS NULL AND end_date IS NULL))
);
CREATE INDEX idx_school_event_dates ON university_event(university_id, start_date, end_date);
CREATE INDEX idx_school_event_times ON university_event(university_id, starts_at, ends_at);
CREATE INDEX idx_personal_event_dates ON personal_event(member_id, start_date, end_date);
CREATE INDEX idx_personal_event_times ON personal_event(member_id, starts_at, ends_at);
CREATE TABLE calendar_subscription (
    member_id BIGINT NOT NULL REFERENCES member(id),
    university_id BIGINT NOT NULL REFERENCES global_universities(id),
    PRIMARY KEY(member_id, university_id)
);
CREATE TABLE university_calendar_manager (
    member_id BIGINT NOT NULL REFERENCES member(id),
    university_id BIGINT NOT NULL REFERENCES global_universities(id),
    PRIMARY KEY(member_id, university_id)
);
CREATE TABLE university_application (
    id BIGSERIAL PRIMARY KEY, version BIGINT NOT NULL DEFAULT 0,
    member_id BIGINT NOT NULL REFERENCES member(id),
    university_id BIGINT NOT NULL REFERENCES global_universities(id),
    department VARCHAR(200) NOT NULL, contact_email VARCHAR(254) NOT NULL, message VARCHAR(2000) NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('PENDING','APPROVED','REJECTED','REVOKED')),
    active_slot BOOLEAN, review_note VARCHAR(2000), reviewed_by VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL, reviewed_at TIMESTAMPTZ,
    UNIQUE(member_id, university_id, active_slot),
    CHECK ((status IN ('PENDING','APPROVED') AND active_slot IS TRUE) OR (status IN ('REJECTED','REVOKED') AND active_slot IS NULL))
);
CREATE INDEX idx_university_application_status ON university_application(status, id);
-- No FK: preserve change records even after a draft is deleted.
CREATE TABLE calendar_audit (
    id BIGSERIAL PRIMARY KEY, aggregate_type VARCHAR(30) NOT NULL, aggregate_id BIGINT NOT NULL,
    actor VARCHAR(255) NOT NULL, action VARCHAR(30) NOT NULL, snapshot TEXT NOT NULL, created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_calendar_audit_aggregate ON calendar_audit(aggregate_type, aggregate_id);
