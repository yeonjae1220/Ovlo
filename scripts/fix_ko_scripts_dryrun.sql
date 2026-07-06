-- (1b) ko 비가나 스크립트 파편 교정 — 고신뢰 ~40종. 끝 ROLLBACK=dry-run / COMMIT=적용.
\pset pager off
BEGIN;
CREATE TEMP TABLE fix(bad text, good text);
INSERT INTO fix(bad, good) VALUES
  ('에드ิน버러','에든버러'),('샤크шу카','샤크슈카'),
  ('компак트합니다','콤팩트합니다'),('компак트한','콤팩트한'),
  ('كي요토','교토'),('미게ль','미겔'),('헬وان','헬완'),
  ('스тир링','스털링'),('아jmาน','아지만'),('펜анг','페낭'),
  ('수쿰вит','수쿰빗'),('Виза','비자'),('виза','비자'),
  ('วีซ่านักศึกษา','학생 비자'),('แฟ어팩스','페어팩스'),('แฮ밀턴','해밀턴'),
  ('스แควร์','스퀘어'),('시르니ки','시르니키'),('ส้มตำ','솜땀'),
  ('дир함','디르함'),('데브레цен','데브레첸'),('데스ерт','디저트'),
  ('링он베리','링곤베리'),('무ปิง','무핑'),('뮤ллер','뮐러'),
  ('루브ек','뤼베크'),('खुल्ना','쿨나'),('페르ليس','페를리스'),
  ('라와ڠ','라왕'),('리ڠGIT','링깃'),('고레ڠ','고렝'),
  ('자바하라ल','자와할랄'),('콜ом나','콜롬나'),('헤스лин고르','헬싱괴르'),
  ('카르토шка','카르토시카'),('говядина','소고기'),('площадь','광장'),
  ('Киев','키예프'),('스트로우와เฟ르','스트룹와플'),('스니츠ель','슈니첼'),
  ('시бирская','시비르스카야'),('데브레','데브레'),('디кси','딕시'),
  ('로젠бор그','로젠보르'),('차니гар','찬디가르'),('라ल바그','랄바그');

SELECT 'BEFORE' AS phase, count(*) AS rows
FROM university_report_translation
WHERE lang='ko' AND concat_ws(' ',title,coalesce(summary,''),body,content::text) ~ '[Ѐ-ӿก-๛ऀ-ॿ؀-ۿ]';

DO $$
DECLARE r record;
BEGIN
  FOR r IN SELECT bad, good FROM fix LOOP
    UPDATE university_report_translation SET
      title=replace(title,r.bad,r.good), summary=replace(summary,r.bad,r.good),
      body=replace(body,r.bad,r.good), content=replace(content::text,r.bad,r.good)::jsonb
    WHERE lang='ko' AND (title LIKE '%'||r.bad||'%' OR summary LIKE '%'||r.bad||'%'
       OR body LIKE '%'||r.bad||'%' OR content::text LIKE '%'||r.bad||'%');
  END LOOP;
END $$;

SELECT 'AFTER' AS phase, count(*) AS rows
FROM university_report_translation
WHERE lang='ko' AND concat_ws(' ',title,coalesce(summary,''),body,content::text) ~ '[Ѐ-ӿก-๛ऀ-ॿ؀-ۿ]';

ROLLBACK;
