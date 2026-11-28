-- ============================================================================
-- V60: 고려대학교(KR) 영문 국가명 교정 (country_en 'Japan' → 'Korea, Republic of')
-- ----------------------------------------------------------------------------
-- 고려대 행(5056)은 원본 데이터셋에서 일본 朝鮮大学校(Korea University, JP) 정보와 섞여 있었다.
-- V40 이 도메인(korea.ac.kr)을, V55 가 한국어 국가명(country)을 고쳤지만 영문 국가명(country_en)은
-- 'Japan' 으로 남았다. 2026-11-28 운영 실측: country_code='KR' 이면서 country_en 이 'Japan' 인 행은
-- 이 1행뿐이고, 나머지 한국 대학은 'Korea, Republic of' 를 쓴다.
-- V55 와 같은 방식: 학교 식별(id·name_en·country_code)과 바꾸기 전 값을 함께 맞춘다.
-- 일본의 Korea University(JP) 행은 건드리지 않는다. country(V55 의 '대한민국')는 그대로 둔다.
-- 빈 환경(테스트 DB)에서는 no-op. 되돌리기: country_en = 'Japan'.
-- ============================================================================

UPDATE global_universities
SET country_en = 'Korea, Republic of'
WHERE id = 5056
  AND name_en = 'Korea University'
  AND country_code = 'KR'
  AND country_en IN ('Japan', '日本', '일본');
