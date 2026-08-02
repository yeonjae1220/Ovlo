-- ============================================================================
-- ko 리포트 "자기 대학명 순수-한글 음차 파손" 교정 (2026-07-25)
--   트리거: 서강대(248) ko body가 "소랑 대학교"로 표기됨(스크립트 혼입 아님).
--   탐지: title(2026-07-06 재생성 정본) 어근이 body에 없고, body가 다른
--         "[가-힣]{2,6}대학" 토큰을 쓰는 ko 리포트 = 자기대학명 오염 후보.
--   원칙(GLOBAL-PIT-063): 좁은 리터럴 치환·백업·dry-run 후 적용·재검증.
--   범위: 명백한 5건만. 서술형 번역명(317/999)·타교언급 오역(1097)·
--         마스터데이터 모호(156)는 판단 유보로 제외.
-- ============================================================================

BEGIN;

-- 백업 (원본 전 필드)
CREATE TABLE IF NOT EXISTS _bak_ko_selfname_20260725 AS
SELECT * FROM university_report_translation
WHERE lang='ko' AND report_id IN (5,222,248,304,991);

-- 적용
UPDATE university_report_translation SET body = replace(body,'한국대학교','고려대학교')       WHERE lang='ko' AND report_id=5;   -- Korea University
UPDATE university_report_translation SET body = replace(body,'동양정립대학','화동사범대학')   WHERE lang='ko' AND report_id=222; -- ECNU
UPDATE university_report_translation SET body = replace(body,'소랑대학교','서강대학교')       WHERE lang='ko' AND report_id=248; -- Sogang
UPDATE university_report_translation SET body = replace(body,'서울여자대학교','숙명여자대학교') WHERE lang='ko' AND report_id=304; -- Sookmyung (실존 타교 스왑)
UPDATE university_report_translation SET body = replace(body,'고려대학교','국민대학교')       WHERE lang='ko' AND report_id=991; -- Kookmin (실존 타교 스왑)

-- 재검증: 오기 잔존 0
SELECT report_id,
  (body ~ ANY(ARRAY['한국대학교','동양정립대학','소랑대학교','서울여자대학교'])) AS resid,
  (report_id=991 AND body ~ '고려대학교') AS r991_resid
FROM university_report_translation
WHERE lang='ko' AND report_id IN (5,222,248,304,991) ORDER BY report_id;

COMMIT;

-- ============================================================================
-- 추가분 (사용자 결정 후)
--   1097: 자기대학(인민대)이 아니라 본문이 언급한 베이징사범대의 "Normal"을
--         "정상"으로 오역 → "사범"으로 교정.
--   156 : 마스터 링크는 정확(Southeast University / 방글라데시 BD)이나 title
--         재생성이 동명 중국 东南大学(Wikidata Q3551770) 라벨을 끌어옴.
--         잘못된 global_university_names 6행 삭제 + title 6언어 재조립.
-- ============================================================================

BEGIN;
CREATE TABLE IF NOT EXISTS _bak_ko_selfname_20260725_b AS
  SELECT * FROM university_report_translation WHERE lang='ko' AND report_id=1097;
UPDATE university_report_translation SET body=replace(body,'정상대학','사범대학')
  WHERE lang='ko' AND report_id=1097;                                             -- Beijing Normal Univ 언급 오역
COMMIT;

BEGIN;
CREATE TABLE IF NOT EXISTS _bak_gun_1704_20260725   AS SELECT * FROM global_university_names WHERE global_univ_id=1704;
CREATE TABLE IF NOT EXISTS _bak_title156_20260725   AS SELECT * FROM university_report_translation WHERE report_id=156;
-- title 재조립: 잘못된 name-part만 정본 name_en로 치환, 언어별 접미사는 보존
UPDATE university_report_translation t
SET title = 'Southeast University' || replace(t.title, g.name, '')
FROM global_university_names g
WHERE g.global_univ_id=1704 AND g.lang=t.lang AND t.report_id=156;
-- 동명 중국교(Q3551770) 라벨 6행 삭제 → 검색/제목 name_en 폴백
DELETE FROM global_university_names WHERE global_univ_id=1704;
COMMIT;

-- ============================================================================
-- 비-ko 교차확인 (2026-07-25) — ko에서 고친 리포트가 다른 언어 body/summary에도
--   같은 엔티티 오염을 가진 것을 발견. 언어별 검증 정본명(global_university_names)
--   을 앵커로 사용. content엔 전부 없음. 좁은 리터럴, 백업 후 적용.
--   r5  Korea University = 高麗/Goryeo (국가명 "한국/韓国/韩国"으로 오역)
--   r222 ECNU = 華東(East China) (vi가 "Đông Nam"=Southeast로 오역)
--   r304 Sookmyung (zh가 "首尔女子"=Seoul Women's 실존 타교로 스왑)
--   r991 Kookmin (zh 헤더가 "韩国大学"=Korea Univ; "（第一部分）" 누출은 별개·미수정)
-- ============================================================================
BEGIN;
CREATE TABLE IF NOT EXISTS _bak_nonko_selfname_20260725 AS
  SELECT * FROM university_report_translation
  WHERE (report_id=5 AND lang IN ('ja','zh','vi')) OR (report_id=222 AND lang='vi')
     OR (report_id=304 AND lang='zh') OR (report_id=991 AND lang='zh');

UPDATE university_report_translation SET body=replace(body,'韓国大学','高麗大学'), summary=replace(summary,'韓国大学','高麗大学') WHERE report_id=5 AND lang='ja';
UPDATE university_report_translation SET body=replace(body,'韩国大学','高丽大学'), summary=replace(summary,'韩国大学','高丽大学') WHERE report_id=5 AND lang='zh';
UPDATE university_report_translation SET body=replace(body,'Đại học Hàn Quốc','Đại học Korea'), summary=replace(summary,'Đại học Hàn Quốc','Đại học Korea') WHERE report_id=5 AND lang='vi';
UPDATE university_report_translation SET body=replace(body,'Đại học Đông Nam Trung Quốc','Đại học Sư phạm Hoa Đông') WHERE report_id=222 AND lang='vi';
UPDATE university_report_translation SET body=replace(body,'首尔女子大学','淑明女子大学'), summary=replace(summary,'首尔女子大学','淑明女子大学') WHERE report_id=304 AND lang='zh';
UPDATE university_report_translation SET body=replace(body,'韩国大学','国民大学') WHERE report_id=991 AND lang='zh';
COMMIT;

-- ============================================================================
-- 전면 비-ko 크로스언어 감사 (2026-07-25) — gun-core + 번체/간체 정규화 탐지기로
--   ko는 정상인데 ja/zh만 오염된 리포트까지 발굴. 크로스언어 다수결(+괄호 약자
--   KNU/CNU/SKKU/USP…)로 정답 확정. 검증 정본명(global_university_names) 앵커.
-- ----------------------------------------------------------------------------
-- 207 ja: 慶熙(Kyung Hee)가 "韓国大学"으로 자기명 오역
BEGIN;
CREATE TABLE IF NOT EXISTS _bak_nonko_selfname_20260725_b AS SELECT * FROM university_report_translation WHERE report_id=207 AND lang='ja';
UPDATE university_report_translation SET body=replace(body,'韓国大学','慶熙大学'), summary=replace(summary,'韓国大学','慶熙大学') WHERE report_id=207 AND lang='ja';
COMMIT;

-- 마스터 FK 오링크 재지정 + title 재생성 (사용자 승인). 정답 global 행 미사용→유니크 충돌 없음.
--   r802 ITESO(6063)→Tec de Monterrey(6062), r208 Sciences Po Rennes(9704)→Paris(3320).
--   옛 엔티티 마커 남은 title만 name_en 폴백 재생성(정답 ko title 보존).
BEGIN;
CREATE TABLE IF NOT EXISTS _bak_fk_repoint_20260725_ur AS SELECT * FROM university_report WHERE id IN (208,802);
CREATE TABLE IF NOT EXISTS _bak_fk_repoint_20260725_tr AS SELECT * FROM university_report_translation WHERE report_id IN (208,802);
UPDATE university_report SET global_univ_id=6062 WHERE id=802;
UPDATE university_report SET global_univ_id=3320 WHERE id=208;
WITH sfx(lang,s) AS (VALUES ('de',' Austauschführer'),('en',' Exchange Student Guide'),('fr',' Guide d''échange'),
  ('ja','交換学生ガイド'),('ko',' 교환학생 가이드'),('vi',' Trao đổi sinh viên'),('zh','交换生指南')),
mark(rid,pat) AS (VALUES (802,'Occidente|ITESO|occidental|西部|高等教育'),(208,'Rennes|レンヌ|雷恩')),
gen AS (SELECT t.report_id,t.lang,gu.name_en||sfx.s AS new_title
  FROM university_report_translation t JOIN university_report r ON r.id=t.report_id
  JOIN global_universities gu ON gu.id=r.global_univ_id JOIN sfx ON sfx.lang=t.lang JOIN mark m ON m.rid=t.report_id
  WHERE t.report_id IN (208,802) AND t.title ~ m.pat)
UPDATE university_report_translation t SET title=gen.new_title FROM gen WHERE gen.report_id=t.report_id AND gen.lang=t.lang;
COMMIT;

-- 크로스언어 엔티티 스왑 10건 (검증 정본명, 지명충돌은 컴파운드 리터럴로 좁힘)
BEGIN;
CREATE TABLE IF NOT EXISTS _bak_xlang_swap_20260725 AS SELECT * FROM university_report_translation
  WHERE (report_id=131 AND lang='zh') OR (report_id=172 AND lang='zh') OR (report_id=210 AND lang IN ('zh','ja'))
     OR (report_id=266 AND lang IN ('zh','ko')) OR (report_id=279 AND lang IN ('zh','ja','ko')) OR (report_id=967 AND lang='zh');
UPDATE university_report_translation SET body=replace(body,'庆尚','江原') WHERE report_id=131 AND lang='zh';           -- Kangwon
UPDATE university_report_translation SET body=replace(body,'伯南布哥','米纳斯吉拉斯') WHERE report_id=172 AND lang='zh'; -- Minas Gerais(Pernambuco 오역)
UPDATE university_report_translation SET body=replace(body,'忠南','全南') WHERE report_id=210 AND lang='zh';           -- Chonnam(충남 오역)
UPDATE university_report_translation SET body=replace(body,'釜山国立大学','全南国立大学') WHERE report_id=210 AND lang='ja'; -- Chonnam(부산 오역)
UPDATE university_report_translation SET body=replace(body,'首尔国立大学','成均馆大学') WHERE report_id=266 AND lang='zh'; -- Sungkyunkwan(서울대 오역)
UPDATE university_report_translation SET body=replace(body,'스성공대','성균관대') WHERE report_id=266 AND lang='ko';    -- ko 자기명 파손
UPDATE university_report_translation SET body=replace(body,'庆北大学','岭南大学'), summary=replace(summary,'庆北大学','岭南大学') WHERE report_id=279 AND lang='zh'; -- Yeungnam(경북 오역)
UPDATE university_report_translation SET body=replace(body,'慶南大学','嶺南大学'), summary=replace(summary,'慶南大学','嶺南大学') WHERE report_id=279 AND lang='ja'; -- Yeungnam(경남 오역)
UPDATE university_report_translation SET body=replace(body,'예당대학교','영남대학교') WHERE report_id=279 AND lang='ko'; -- ko 자기명 파손
UPDATE university_report_translation SET body=replace(body,'东京科学大学','芝浦工业大学') WHERE report_id=967 AND lang='zh'; -- Shibaura
COMMIT;

-- ja 추가 스왑 4건 (부경대↔부산대·华东↔东华 등 유명 혼동)
BEGIN;
CREATE TABLE IF NOT EXISTS _bak_ja_swap_20260725 AS SELECT * FROM university_report_translation WHERE lang='ja' AND report_id IN (346,410,1033,984);
UPDATE university_report_translation SET body=replace(body,'釜山国立大学','釜慶大学'), summary=replace(summary,'釜山国立大学','釜慶大学') WHERE report_id=346 AND lang='ja'; -- Pukyong(부산대 오역)
UPDATE university_report_translation SET body=replace(body,'中国香港大学','香港中文大学'), summary=replace(summary,'中国香港大学','香港中文大学') WHERE report_id=410 AND lang='ja'; -- CUHK
UPDATE university_report_translation SET body=replace(body,'東華大学','華東理工大学') WHERE report_id=1033 AND lang='ja'; -- ECUST(东华 오역)
UPDATE university_report_translation SET body=replace(body,'広州大学','中山大学') WHERE report_id=984 AND lang='ja';   -- SYSU
COMMIT;

-- ⚠️ 미완(별도 후속): r364 São Paulo가 Pula로 오링크지만 정답 global 2021(USP)이 이미
--    report 564에 연결돼 유니크 제약으로 재지정 불가(중복 USP 리포트 병합 판단 필요).
--    r984 ko "선일대학교"·r410 ko "중국대학교 홍콩" 등 ko 자기명 잔여, ja/zh 롱테일 미완.
