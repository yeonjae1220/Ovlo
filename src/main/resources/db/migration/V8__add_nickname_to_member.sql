ALTER TABLE member ADD COLUMN nickname VARCHAR(50);

-- 기존 회원에게 기본 닉네임 부여 (email 앞부분 + id)
UPDATE member SET nickname = SPLIT_PART(email, '@', 1) || '_' || id WHERE nickname IS NULL;

ALTER TABLE member ALTER COLUMN nickname SET NOT NULL;

CREATE UNIQUE INDEX idx_member_nickname ON member (nickname);
