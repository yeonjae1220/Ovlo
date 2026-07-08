-- ============================================================================
-- ko 리포트 한자 혼입 2차 교정: 잔존 140개 리포트(엄격 인접 규칙 검증) (2026-07-08)
-- ----------------------------------------------------------------------------
-- 1차(fix_han_in_ko_apply.sql)는 고빈도 공통 패턴만 처리했다. 나머지는
-- global_universities/exchange_universities(name_en+country)로 대학 소재지를
-- 대조해 지명/자기 대학명 오기(모纳什=Monash 자기 대학명, 합肥=USTC 소재 도시,
-- 京都=교토대 자기 도시명 등 — 부경대 346과 같은 '자기 대학/도시명 오염' 패턴이
-- 여러 건 더 있었다)를 확인 후 처리했다.
-- 중요: 느슨한 정규식(한자 앞뒤 0개 허용)으로 뽑은 1차 후보에는 오탐이 많았다
-- (예: "武昌鱼市(Wuchang Fish Market)"처럼 이미 괄호로 정상 병기된 것도 걸림).
-- `[가-힣一-鿿]+` 최대런 매칭 + 양쪽 문자셋 존재 필터로 재검증한 185개 실제
-- 조각(base-form 축약 시 145개 사전)만 교정한다. 모든 원문은 DB
-- 쿼리 결과에서 unaligned/tuples-only로 줄바꿈 없이 추출해(이전 wrapped 출력
-- 파싱이 몇 건을 누락시켰음을 dry-run으로 발견 후 재작업) 수기 재입력 없이
-- 사전을 만들었다.
-- 확정 불가 9건은 "(?)" 마커로 남기고 report_corrections.sql에 등록한다.
-- 안전장치: UPDATE 전 백업. 전부 리터럴 문자열 치환(정규식 미사용). 적용 전
-- dry-run(SELECT만)으로 전체 728행 재검증 — 잔존 0건 확인 후 실행.
-- ============================================================================

\pset pager off

DROP TABLE IF EXISTS _bak_han_in_ko_round2_20260708;
CREATE TABLE _bak_han_in_ko_round2_20260708 AS
SELECT report_id, lang, title, summary, body, content
FROM university_report_translation
WHERE lang = 'ko';

CREATE OR REPLACE FUNCTION _fix_han_in_ko_r2(t TEXT) RETURNS TEXT AS $$
    SELECT replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(replace(t, '디오克莱티아누스', '디오클레티아누스'), '퀸메스国立대학교', '퀸메스 국립대학교'), '가입하여室友를', '가입하여 룸메이트를'), '델프트理工大学', '델프트 공과대학교'), '베이징理工大学', '베이징 이공대학교'), '샤리프理工大学', '샤리프 공과대학교'), '식사自理하세요', '식사는 스스로 해결하세요'), '알아卜둘라지즈', '알 압둘아지즈'), '이탈리아室友와', '이탈리아 룸메이트와'), '콜롬비아室友과', '콜롬비아 룸메이트와'), '聖帕特里克의', '성 패트릭의'), '구아야퀴尔은', '과야킬'), '국가森林公园', '삼림공원'), '그라블라克斯', '그라블락스'), '브리티시어姆', '버밍엄(?)'), '식당就餐보다', '식당에서 식사하는 것보다'), '아卜둘아지즈', '압둘아지즈'), '아프리카버恩', '아프리카번'), '이슬라마巴드', '이슬라마바드'), '전자科技大学', '전자과학기술대학교'), '주로阿拉伯어', '주로 아랍어'), '체코인室友와', '체코인 룸메이트와'), '팁으로는室友', '팁으로는 룸메이트'), '패노노哈尔미', '판노하르마'), '吉林대학교는', '지린'), '이슬라마巴德', '이슬라마바드'), '厦門대학교', '샤먼'), '瓜达拉哈라', '과달라하라'), '달콤한红薯', '달콤한 고구마'), '로봇俱樂部', '로봇 동아리'), '보통室友와', '보통 룸메이트와'), '빌니우纳斯', '빌뉴스'), '산安东尼오', '산안토니오'), '상하이中心', '상하이타워'), '상하이浦东', '상하이 푸둥'), '아卜달라리', '압돌라히(?)'), '아姆斯特丹', '암스테르담'), '아姆스트덴', '암스테르담'), '아姆스트롱', '암스테르담'), '아巴拉치안', '애팔래치아'), '예想不到을', '예상치 못한 것을'), '조엘这样的', '조엘 같은'), '주로室友와', '주로 룸메이트와'), '토론俱樂部', '토론 동아리'), '헤르福德셔', '하트퍼드셔'), '구아야퀴尔', '과야킬'), '아姆스트丹', '암스테르담'), '孔子사당', '공자사당'), '공공舆论', '공공여론'), '나츄라尔', '나투랄'), '나피维尔', '내슈빌'), '뉴卡스널', '뉴캐슬'), '데바迪拜', '두바이'), '데브레岑', '데브레첸'), '디브레岑', '데브레첸'), '맨彻스터', '맨체스터'), '메尔버른', '멜버른'), '명의室友', '명의 룸메이트'), '바克拉바', '바클라바'), '브란덴堡', '브란덴부르크'), '블루메瑙', '블루메나우'), '쇠고기串', '쇠고기 꼬치'), '스特朗트', '스트란트(?)'), '스芬크스', '스핑크스'), '스티尔斯', '스티어스(?)'), '아卜둘라', '압둘라'), '아레纳斯', '아레나스'), '알卑스가', '알프스가'), '알坎타라', '알칸타라'), '알罕布拉', '알함브라'), '에센勒르', '에센레르'), '연합酋에', '에미리트에'), '오姆스크', '옴스크'), '올로무茨', '올로모우츠'), '위니辟그', '위니펙'), '이스파翰', '이스파한'), '채플希尔', '채플힐'), '카라干다', '카라간다'), '카르투姆', '하르툼'), '타姆페레', '탐페레'), '타크시姆', '탁심'), '토姆스크', '톰스크'), '하트워恩', '하트워른(?)'), '혜택争取', '혜택 쟁취'), '森林公园', '삼림공원'), '京都는', '교토'), '朝阳구', '차오양구'), '담마姆', '담맘'), '레이姆', '레임(?)'), '모纳什', '모나시'), '산胡안', '산후안'), '살라姆', '살람'), '생魷魚', '생오징어'), '생미歇', '생미셸'), '수宫殿', '수상 궁전'), '스旺시', '스완지'), '스旺지', '스완지'), '신天地', '신톈디'), '신干线', '신칸센'), '아卜드', '압둘'), '아缦하', '아만하(?)'), '아联酋', '아랍에미리트'), '안卡拉', '앙카라'), '에菲尔', '에펠'), '자취饪', '자취'), '재省钱', '재정 절약'), '카레姆', '카림'), '카시姆', '카심'), '크레姆', '크림'), '탄데姆', '탄뎀'), '탄뎀姆', '탄뎀'), '태晤스', '템스'), '터姆과', '텀(?)과'), '터姆을', '텀(?)을'), '파레莫', '팔레르모'), '파이尔', '실패'), '팔레莫', '팔레르모'), '하레姆', '할렘'), '할레姆', '할렘'), '해淀구', '하이뎬'), '해珠구', '샹저우구(?)'), '황浦강', '황푸강'), '황浦구', '황푸구'), '흰米饭', '흰쌀밥'), '힐레尔', '힐렐'), '长春은', '창춘'), '라姆', '람'), '라尔', '랄(?)'), '루姆', '룸'), '리亚', '리알'), '민谣', '민요'), '산璜', '산후안'), '아琛', '아헨'), '야姆', '아얌'), '유姆', '얌'), '장沙', '창사'), '지聘', '즈핀(?)'), '틸堡', '틸뷔르흐'), '합肥', '허페이'), '京都', '교토'), '厦門', '샤먼'), '厦门', '샤먼'), '吉林', '지린'), '长春', '창춘'), '해淀', '하이뎬');
$$ LANGUAGE SQL IMMUTABLE;

UPDATE university_report_translation t
SET
    title   = _fix_han_in_ko_r2(t.title),
    summary = CASE WHEN t.summary IS NULL THEN NULL ELSE _fix_han_in_ko_r2(t.summary) END,
    body    = _fix_han_in_ko_r2(t.body),
    content = _fix_han_in_ko_r2(t.content::text)::jsonb
WHERE t.lang = 'ko';

DROP FUNCTION _fix_han_in_ko_r2(TEXT);

-- ── 결과 확인: 엄격 인접 규칙 기준 잔존 건수 ──────────────────────────────────
SELECT count(distinct report_id) AS still_affected_reports
FROM university_report_translation
WHERE lang='ko'
  AND (title ~ '[가-힣][一-鿿]|[一-鿿][가-힣]'
    OR summary ~ '[가-힣][一-鿿]|[一-鿿][가-힣]'
    OR body ~ '[가-힣][一-鿿]|[一-鿿][가-힣]'
    OR content::text ~ '[가-힣][一-鿿]|[一-鿿][가-힣]');
