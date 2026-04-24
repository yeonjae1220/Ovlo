-- V17: exchange_universities.country_code를 한국어 country 컬럼 기준으로 전면 재정규화
-- V16 Pass 1(FK JOIN)이 global_universities의 잘못된 데이터를 가져온 케이스 수정
-- 한국어 country 컬럼이 더 신뢰할 수 있는 소스

UPDATE exchange_universities
SET country_code = CASE country
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
WHERE country IS NOT NULL;
