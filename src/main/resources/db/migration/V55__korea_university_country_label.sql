-- Verified against https://www.korea.ac.kr/sites/en/index.do (2026-11-24).
-- V40 corrected this Korean university's domain; its old Japanese country label remained.
-- Match both identity and previous value. Never rewrite the Japanese Korea University (JP).
UPDATE global_universities
SET country='대한민국'
WHERE id=5056 AND name_en='Korea University' AND country_code='KR'
  AND country IN ('일본','Japan','日本');

UPDATE exchange_universities
SET country='대한민국'
WHERE global_univ_id=5056 AND country_code='KR'
  AND country IN ('일본','Japan','日本');
