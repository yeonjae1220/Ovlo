-- V21: 구 university(40) 테이블 폐기 + 멤버 본교 FK를 global_universities로
-- ============================================================================
-- 멤버 본교 선택이 global_universities(10,150)로 일원화 완료됨(V19/V20).
-- 코드(University 도메인/어댑터)도 global_universities 백킹으로 전환됨.
--   - member.home_university_id 참조 무결성 보장(FK) → 존재하지 않는 대학 차단
--   - 더 이상 참조되지 않는 university 40-테이블 제거
-- 선행 보장: V20이 모든 member.home_university_id를 유효한 global id로 정합화함.
-- ============================================================================

ALTER TABLE member
    ADD CONSTRAINT fk_member_home_university
    FOREIGN KEY (home_university_id) REFERENCES global_universities (id);

DROP TABLE IF EXISTS university;
