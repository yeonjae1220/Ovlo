-- V19: global_universities 좌표/현지어명 보강 + 누락 대학 추가 + 이메일 검증 인덱스
-- ============================================================================
-- 목적: 멤버 본교 선택을 university(40) → global_universities(10,150) 로 일원화.
--   - university(40)가 보유하던 좌표(lat/lng)·현지어명(local_name)을 global로 이전
--   - global에 없던 인기 대학(Sorbonne, PSL) 신규 추가
--   - 학교 이메일 검증용 domain 조회 인덱스(NON-UNIQUE: domain→대학 1:N 가능)
-- 주의: 좌표는 university 테이블을 JOIN하지 않고 global_id 기준 명시값으로 박는다.
--       (university 테이블은 후속 마이그레이션에서 DROP 되므로 의존성 제거)
-- ============================================================================

-- ── 1. 컬럼 추가 ─────────────────────────────────────────────────────────────
ALTER TABLE global_universities ADD COLUMN IF NOT EXISTS latitude   DOUBLE PRECISION;
ALTER TABLE global_universities ADD COLUMN IF NOT EXISTS longitude  DOUBLE PRECISION;
ALTER TABLE global_universities ADD COLUMN IF NOT EXISTS local_name VARCHAR(300);

-- ── 2. 40개 큐레이션 대학의 좌표·현지어명 병합 (global_id 기준 명시) ──────────────
--    이름/웹사이트 매칭으로 검증된 37개 (fan-out 0, 1:1 확인 완료)
UPDATE global_universities SET latitude=40.7295, longitude=-73.9965, local_name='NYU' WHERE id=700;
UPDATE global_universities SET latitude=37.8724, longitude=-122.2595, local_name='UC Berkeley' WHERE id=1000;
UPDATE global_universities SET latitude=34.0689, longitude=-118.4452, local_name='UCLA' WHERE id=1003;
UPDATE global_universities SET latitude=42.278, longitude=-83.7382, local_name='UMich' WHERE id=1068;
UPDATE global_universities SET latitude=30.2849, longitude=-97.7341, local_name='UT Austin' WHERE id=1139;
UPDATE global_universities SET latitude=-35.2777, longitude=149.1185, local_name='ANU' WHERE id=1511;
UPDATE global_universities SET latitude=-37.7963, longitude=144.9614 WHERE id=1542;
UPDATE global_universities SET latitude=-33.8888, longitude=151.1873 WHERE id=1549;
UPDATE global_universities SET latitude=45.5048, longitude=-73.5772, local_name='McGill' WHERE id=2155;
UPDATE global_universities SET latitude=49.2606, longitude=-123.246, local_name='UBC' WHERE id=2197;
UPDATE global_universities SET latitude=43.6629, longitude=-79.3957, local_name='U of T' WHERE id=2233;
UPDATE global_universities SET latitude=52.5186, longitude=13.3933, local_name='HU Berlin' WHERE id=3590;
UPDATE global_universities SET latitude=48.1496, longitude=11.5677, local_name='TUM' WHERE id=3657;
UPDATE global_universities SET latitude=48.1508, longitude=11.5805, local_name='LMU München' WHERE id=3697;
UPDATE global_universities SET latitude=22.2835, longitude=114.1363, local_name='HKU' WHERE id=3828;
UPDATE global_universities SET latitude=22.3363, longitude=114.2635, local_name='HKUST' WHERE id=3832;
UPDATE global_universities SET latitude=35.6459, longitude=139.7358, local_name='慶應義塾大学' WHERE id=5021;
UPDATE global_universities SET latitude=37.5894, longitude=127.0322, local_name='고려대학교' WHERE id=5056;
UPDATE global_universities SET latitude=35.0263, longitude=135.7806, local_name='京都大学' WHERE id=5090;
UPDATE global_universities SET latitude=34.8219, longitude=135.5243, local_name='大阪大学' WHERE id=5207;
UPDATE global_universities SET latitude=35.7128, longitude=139.761, local_name='東京大学' WHERE id=5385;
UPDATE global_universities SET latitude=35.709, longitude=139.7195, local_name='早稲田大学' WHERE id=5391;
UPDATE global_universities SET latitude=36.3725, longitude=127.36, local_name='한국과학기술원' WHERE id=5619;
UPDATE global_universities SET latitude=37.4601, longitude=126.9523, local_name='서울대학교' WHERE id=5703;
UPDATE global_universities SET latitude=37.587, longitude=126.993, local_name='성균관대학교' WHERE id=5727;
UPDATE global_universities SET latitude=37.5656, longitude=126.9385, local_name='연세대학교' WHERE id=5744;
UPDATE global_universities SET latitude=51.9986, longitude=4.3731, local_name='TU Delft' WHERE id=6304;
UPDATE global_universities SET latitude=52.3667, longitude=4.9, local_name='UvA' WHERE id=6310;
UPDATE global_universities SET latitude=1.3483, longitude=103.6831, local_name='NTU' WHERE id=7543;
UPDATE global_universities SET latitude=1.2966, longitude=103.7764, local_name='NUS' WHERE id=7544;
UPDATE global_universities SET latitude=59.3498, longitude=18.0707, local_name='KTH' WHERE id=7803;
UPDATE global_universities SET latitude=59.365, longitude=18.0582 WHERE id=7816;
UPDATE global_universities SET latitude=52.2043, longitude=0.1149, local_name='Cambridge' WHERE id=8355;
UPDATE global_universities SET latitude=55.9445, longitude=-3.1892, local_name='Edinburgh' WHERE id=8370;
UPDATE global_universities SET latitude=53.4668, longitude=-2.2339 WHERE id=8415;
UPDATE global_universities SET latitude=51.7548, longitude=-1.2544, local_name='Oxford' WHERE id=8427;
UPDATE global_universities SET latitude=51.5245, longitude=-0.1339, local_name='UCL' WHERE id=8468;

-- Sciences Po: 이름/웹 매칭 실패했으나 global에 존재(3320) → 좌표만 보강
UPDATE global_universities SET latitude=48.8545, longitude=2.3282, local_name='Sciences Po'
WHERE id=3320 AND domain='sciences-po.fr';

-- ── 3. global에 없던 인기 대학 신규 추가 (domain 기준 멱등) ───────────────────────
INSERT INTO global_universities (name_en, country, country_en, country_code, city, website, domain, latitude, longitude, local_name)
SELECT 'Sorbonne University', '프랑스', 'France', 'FR', 'Paris',
       'https://www.sorbonne-universite.fr', 'sorbonne-universite.fr', 48.8509, 2.3439, 'Sorbonne'
WHERE NOT EXISTS (SELECT 1 FROM global_universities WHERE domain='sorbonne-universite.fr');

INSERT INTO global_universities (name_en, country, country_en, country_code, city, website, domain, latitude, longitude, local_name)
SELECT 'Paris Sciences et Lettres University', '프랑스', 'France', 'FR', 'Paris',
       'https://www.psl.eu', 'psl.eu', 48.8462, 2.3436, 'PSL'
WHERE NOT EXISTS (SELECT 1 FROM global_universities WHERE domain='psl.eu');

-- ── 4. 이메일 검증용 도메인 조회 인덱스 ────────────────────────────────────────
--    NON-UNIQUE: 데이터셋상 동일 domain을 가진 별개 대학이 존재(1:N).
--    검증기는 domain→대학 집합을 받아 멤버 본교로 disambiguate 한다.
CREATE INDEX IF NOT EXISTS idx_gu_domain ON global_universities (lower(domain));

-- ── 5. 좌표 보유 대학 조회용 부분 인덱스 (지도: 좌표 있는 대학만 노출) ──────────────
CREATE INDEX IF NOT EXISTS idx_gu_has_coords ON global_universities (id)
    WHERE latitude IS NOT NULL AND longitude IS NOT NULL;
