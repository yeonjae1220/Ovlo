-- A안: 좋아요/싫어요 비정규화 카운트.
-- 반응은 회원 1명당 post_reaction 1행(PK: post_id, member_id)으로 idempotent 하게 관리된다.
-- 서로 다른 회원의 반응은 서로 다른 행이라 애초에 충돌하지 않고, 유일하게 공유되는 카운트만
-- UPDATE post SET like_count = like_count + :delta 원자적 증감으로 갱신한다. DB가 델타를 원자적으로
-- 적용하므로 @Version 낙관적 락이나 재시도 없이도 동시 반응이 lost update 없이 정확히 수렴한다.
ALTER TABLE post ADD COLUMN like_count    BIGINT NOT NULL DEFAULT 0;
ALTER TABLE post ADD COLUMN dislike_count BIGINT NOT NULL DEFAULT 0;

-- 기존 반응 행으로부터 카운트 백필 (마이그레이션 이전에 쌓인 반응 반영)
UPDATE post p SET
    like_count    = (SELECT count(*) FROM post_reaction r WHERE r.post_id = p.id AND r.reaction_type = 'LIKE'),
    dislike_count = (SELECT count(*) FROM post_reaction r WHERE r.post_id = p.id AND r.reaction_type = 'DISLIKE');
