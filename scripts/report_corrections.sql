-- ============================================================================
-- 불확실 고유명사 교정 레지스트리 (single source of truth, 멱등 재실행)
-- ----------------------------------------------------------------------------
-- ko 리포트에서 정본을 확정 못한 파편은 이미 '추정값(?)' 마커로 치환돼 있다.
-- 정확한 값이 확인되면 아래 confirmed 칸을 채우고 이 스크립트를 재실행하면
-- 마커가 정본으로 치환된다(LIKE 가드로 멱등 — 몇 번이든 안전 재실행).
--
-- 미확인 전수 조회:  WHERE ... LIKE '%(?)%'
-- 현재 39개 마커(2026-07-08, +9 한자혼입 2차 교정에서 추가). 각 마커의 report 위치는 note 참조.
-- 백업: scripts/ko_translations_backup_20260705.csv, in-DB _bak_titles_20260706, _bak_han_in_ko_round2_20260708
-- ============================================================================
\pset pager off
BEGIN;

CREATE TEMP TABLE corr(marker text, confirmed text, note text);
INSERT INTO corr(marker, confirmed, note) VALUES
  ('폴(?)',         NULL, 'r191 보스턴 자유트레일 명소(Paul Revere? / Fall River?)'),
  ('버밍엄(?)',     NULL, 'r259 아스톤대 소재 도시 추정'),
  ('카오산(?)',     NULL, 'r525 방콕 음식거리(Khao San? / Kasem?)'),
  ('카심(?)',       NULL, 'r1032 아지만 UAE 유산마을(Al Qasim?)'),
  ('나흐숄림(?)',   NULL, 'r882 텔아비브 해변(Nahsholim?)'),
  ('피시브레(?)',   NULL, 'r297 아이슬란드 음식(미상)'),
  ('이통다(?)',     NULL, 'r349 대구 역사명소(미상)'),
  ('센얀(?)',       NULL, 'r894 싱가포르 지역(미상)'),
  ('슈니(?)',       NULL, 'r1036 홍콩 슈퍼마켓(미상)'),
  ('아임스부트(?)', NULL, 'r54 캠퍼스 외부 지역(Amstetten? 미상)'),
  ('토프트옵(?)',   NULL, 'r58 에드먼턴 야간 바(rooftop? 미상)'),
  ('부니농(?)',     NULL, 'r86 역사 마을(미상)'),
  ('코시(?)',       NULL, 'r163 이란 음식 카시크-(Khosh? 미상)'),
  ('하하르(?)',     NULL, 'r189 캠퍼스 인근 지역(미상)'),
  ('크라스니예(?)', NULL, 'r324 모스크바 지명(Krasnye Koltso? 붉은 순환?)'),
  ('오라(?)',       NULL, 'r338 Señora 관련(미상)'),
  ('파트미(?)',     NULL, 'r366 음식명(Fatimah? 미상)'),
  ('칸(?)',         NULL, 'r366 태국 음식(미상)'),
  ('배터먼스(?)',   NULL, 'r723 시드니 인근 만(Batemans Bay? 미상)'),
  ('실드(?)',       NULL, 'r745 기숙사 유형(single/double 관련 미상)'),
  ('링구이스타(?)', NULL, 'r757 과목명 linguista(미상)'),
  ('바이워드(?)',   NULL, 'r804 오타와 마켓(Byward? 미상)'),
  ('바르사나(?)',   NULL, 'r869 클루지 인근(Barsana? 미상)'),
  ('쿠리츠카야(?)', NULL, 'r890 등산 명소 고라(미상)'),
  ('스빗(?)',       NULL, 'r890 지명(미상)'),
  ('요(?)',         NULL, 'r897 피츠(Fitzroy? 미상)'),
  ('수크롬(?)',     NULL, 'r1030 방콕 기숙사(미상)'),
  ('헤이관(?)',     NULL, 'r1051 대학 인근 지역(미상)'),
  ('엘호슨(?)',     NULL, 'r1069 지역명(미상)'),
  ('다웁(?)',       NULL, 'r1070 American University 인근(Dubai? 미상)'),
  ('스티어스(?)',   NULL, 'r171 Toronto Metropolitan University 인근 지역(Stirs? 미상)'),
  ('랄(?)',         NULL, 'r328 JNU 델리 관광지(Lal Qila? 미상)'),
  ('즈핀(?)',       NULL, 'r797 베이징외대 채용 앱(Zhipin/直聘? 미상)'),
  ('샹저우구(?)',   NULL, 'r984 중산대 소재 지역(Xiangzhou? 미상)'),
  ('압돌라히(?)',   NULL, 'r1014 시라즈대 페르시아 요리명(미상)'),
  ('텀(?)',         NULL, 'r1024 카세트사트대 태국 음식/지역(Term? 미상)'),
  ('아만하(?)',     NULL, 'r1041 PUC 리우데자네이루 인근 명소(미상)'),
  ('스트란트(?)',   NULL, 'r1082 RIT 로체스터 인근 명소(Strand? 미상)'),
  ('하트워른(?)',   NULL, 'r845 WVU 도서관명(Hartwerth? 미상)');

-- 마커 → 확정값 (confirmed 채워진 것만 치환)
DO $$
DECLARE r record;
BEGIN
  FOR r IN SELECT marker, confirmed FROM corr WHERE confirmed IS NOT NULL AND confirmed <> '' LOOP
    UPDATE university_report_translation SET
      title=replace(title,r.marker,r.confirmed), summary=replace(summary,r.marker,r.confirmed),
      body=replace(body,r.marker,r.confirmed), content=replace(content::text,r.marker,r.confirmed)::jsonb
    WHERE lang='ko' AND (title LIKE '%'||r.marker||'%' OR summary LIKE '%'||r.marker||'%'
       OR body LIKE '%'||r.marker||'%' OR content::text LIKE '%'||r.marker||'%');
  END LOOP;
END $$;

-- 남은 미확인 마커 현황
SELECT report_id,
       substring(concat_ws(' ',title,coalesce(summary,''),body,content::text)
                 from '[가-힣A-Za-z ]{0,8}\(\?\)') AS pending
FROM university_report_translation
WHERE lang='ko' AND concat_ws(' ',title,coalesce(summary,''),body,content::text) LIKE '%(?)%'
ORDER BY report_id;

COMMIT;
