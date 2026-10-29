-- V33: 품질 게이트가 content.sources(출처 목록)를 언어 섞임 검사에서 제외한다.
-- 재생성 리포트는 출처 제목을 원문 그대로 둔다(예: 중국어 영상 제목, 일본어 공식 페이지 제목).
-- 그 제목이 en/de/fr/vi 행의 script_mix_in_latin, ko 행의 kana_in_ko 등으로 오탐되어
-- report_quality_issue 를 채우지 않게 한다. 본문·나머지 content 필드는 그대로 검사한다.

CREATE OR REPLACE FUNCTION trg_report_quality_check() RETURNS TRIGGER AS $$
DECLARE
    d TEXT[];
BEGIN
    d := detect_report_translation_defects(NEW.lang, NEW.title, NEW.summary, NEW.body,
                                           (NEW.content - 'sources')::text);
    IF array_length(d, 1) IS NULL THEN
        DELETE FROM report_quality_issue WHERE report_id = NEW.report_id AND lang = NEW.lang;
    ELSE
        INSERT INTO report_quality_issue(report_id, lang, defects, detected_at)
        VALUES (NEW.report_id, NEW.lang, d, NOW())
        ON CONFLICT (report_id, lang) DO UPDATE SET defects = EXCLUDED.defects, detected_at = NOW();
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 기존 행 중 sources 를 가진 행은 아직 없으므로(재생성 전) 재계산은 필요 없다.
