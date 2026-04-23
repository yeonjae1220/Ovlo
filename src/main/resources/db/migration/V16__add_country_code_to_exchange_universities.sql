-- V16: exchange_universities에 country_code(ISO 3166-1 alpha-2) 컬럼 추가
-- Pass 1: global_universities FK JOIN으로 자동 채움 (향후 신규 데이터도 이 방식으로 처리)
-- Pass 2: FK 미연결 행은 한국어 국가명 → ISO 코드 CASE fallback

ALTER TABLE exchange_universities
    ADD COLUMN country_code CHAR(2);

-- Pass 1: global_univ_id가 연결된 행 자동 채움
UPDATE exchange_universities eu
SET    country_code = gu.country_code
FROM   global_universities gu
WHERE  eu.global_univ_id = gu.id
  AND  gu.country_code IS NOT NULL;

-- Pass 2: 연결 안 된 행 한국어 국가명 → ISO 코드 fallback
UPDATE exchange_universities
SET    country_code = CASE country
    WHEN '미국'       THEN 'US'
    WHEN '영국'       THEN 'GB'
    WHEN '일본'       THEN 'JP'
    WHEN '독일'       THEN 'DE'
    WHEN '프랑스'     THEN 'FR'
    WHEN '호주'       THEN 'AU'
    WHEN '캐나다'     THEN 'CA'
    WHEN '네덜란드'   THEN 'NL'
    WHEN '스웨덴'     THEN 'SE'
    WHEN '싱가포르'   THEN 'SG'
    WHEN '홍콩'       THEN 'HK'
    WHEN '한국'       THEN 'KR'
    WHEN '중국'       THEN 'CN'
    WHEN '스페인'     THEN 'ES'
    WHEN '이탈리아'   THEN 'IT'
    WHEN '스위스'     THEN 'CH'
    WHEN '벨기에'     THEN 'BE'
    WHEN '덴마크'     THEN 'DK'
    WHEN '핀란드'     THEN 'FI'
    WHEN '노르웨이'   THEN 'NO'
    WHEN '오스트리아' THEN 'AT'
    WHEN '뉴질랜드'   THEN 'NZ'
    WHEN '대만'       THEN 'TW'
    WHEN '태국'       THEN 'TH'
    WHEN '말레이시아' THEN 'MY'
    WHEN '베트남'     THEN 'VN'
    WHEN '인도네시아' THEN 'ID'
    WHEN '인도'       THEN 'IN'
    WHEN '멕시코'     THEN 'MX'
    WHEN '브라질'     THEN 'BR'
    WHEN '칠레'       THEN 'CL'
    WHEN '포르투갈'   THEN 'PT'
    WHEN '체코'       THEN 'CZ'
    WHEN '폴란드'     THEN 'PL'
    WHEN '헝가리'     THEN 'HU'
    WHEN '튀르키예'   THEN 'TR'
    WHEN '레바논'     THEN 'LB'
    WHEN '콜롬비아'   THEN 'CO'
    ELSE NULL
END
WHERE country_code IS NULL
  AND country IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_eu_country_code ON exchange_universities(country_code);
