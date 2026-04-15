-- V14: member 테이블에 role 컬럼 추가

ALTER TABLE member ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'MEMBER';

CREATE INDEX idx_member_role ON member (role);
