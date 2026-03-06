-- R__seed_universities.sql
-- Flyway repeatable migration: 파일이 변경될 때마다 재실행
-- 교환학생 인기 대학 시드 데이터

INSERT INTO university (name, local_name, country_code, city, latitude, longitude, website_url)
SELECT * FROM (VALUES
    -- 미국
    ('University of California, Los Angeles', 'UCLA', 'US', 'Los Angeles', 34.0689, -118.4452, 'https://www.ucla.edu'),
    ('University of California, Berkeley', 'UC Berkeley', 'US', 'Berkeley', 37.8724, -122.2595, 'https://www.berkeley.edu'),
    ('New York University', 'NYU', 'US', 'New York', 40.7295, -73.9965, 'https://www.nyu.edu'),
    ('University of Michigan', 'UMich', 'US', 'Ann Arbor', 42.2780, -83.7382, 'https://umich.edu'),
    ('University of Texas at Austin', 'UT Austin', 'US', 'Austin', 30.2849, -97.7341, 'https://www.utexas.edu'),

    -- 영국
    ('University of Oxford', 'Oxford', 'GB', 'Oxford', 51.7548, -1.2544, 'https://www.ox.ac.uk'),
    ('University of Cambridge', 'Cambridge', 'GB', 'Cambridge', 52.2043, 0.1149, 'https://www.cam.ac.uk'),
    ('University College London', 'UCL', 'GB', 'London', 51.5245, -0.1339, 'https://www.ucl.ac.uk'),
    ('The University of Edinburgh', 'Edinburgh', 'GB', 'Edinburgh', 55.9445, -3.1892, 'https://www.ed.ac.uk'),
    ('University of Manchester', NULL, 'GB', 'Manchester', 53.4668, -2.2339, 'https://www.manchester.ac.uk'),

    -- 일본
    ('University of Tokyo', '東京大学', 'JP', 'Tokyo', 35.7128, 139.7610, 'https://www.u-tokyo.ac.jp'),
    ('Kyoto University', '京都大学', 'JP', 'Kyoto', 35.0263, 135.7806, 'https://www.kyoto-u.ac.jp'),
    ('Waseda University', '早稲田大学', 'JP', 'Tokyo', 35.7090, 139.7195, 'https://www.waseda.jp'),
    ('Keio University', '慶應義塾大学', 'JP', 'Tokyo', 35.6459, 139.7358, 'https://www.keio.ac.jp'),
    ('Osaka University', '大阪大学', 'JP', 'Osaka', 34.8219, 135.5243, 'https://www.osaka-u.ac.jp'),

    -- 독일
    ('Ludwig Maximilian University of Munich', 'LMU München', 'DE', 'Munich', 48.1508, 11.5805, 'https://www.lmu.de'),
    ('Humboldt University of Berlin', 'HU Berlin', 'DE', 'Berlin', 52.5186, 13.3933, 'https://www.hu-berlin.de'),
    ('Technical University of Munich', 'TUM', 'DE', 'Munich', 48.1496, 11.5677, 'https://www.tum.de'),

    -- 프랑스
    ('Paris Sciences et Lettres University', 'PSL', 'FR', 'Paris', 48.8462, 2.3436, 'https://www.psl.eu'),
    ('Sciences Po', NULL, 'FR', 'Paris', 48.8545, 2.3282, 'https://www.sciencespo.fr'),
    ('Sorbonne University', 'Sorbonne', 'FR', 'Paris', 48.8509, 2.3439, 'https://www.sorbonne-universite.fr'),

    -- 호주
    ('University of Melbourne', NULL, 'AU', 'Melbourne', -37.7963, 144.9614, 'https://www.unimelb.edu.au'),
    ('University of Sydney', NULL, 'AU', 'Sydney', -33.8888, 151.1873, 'https://www.sydney.edu.au'),
    ('Australian National University', 'ANU', 'AU', 'Canberra', -35.2777, 149.1185, 'https://www.anu.edu.au'),

    -- 캐나다
    ('University of Toronto', 'U of T', 'CA', 'Toronto', 43.6629, -79.3957, 'https://www.utoronto.ca'),
    ('McGill University', 'McGill', 'CA', 'Montreal', 45.5048, -73.5772, 'https://www.mcgill.ca'),
    ('University of British Columbia', 'UBC', 'CA', 'Vancouver', 49.2606, -123.2460, 'https://www.ubc.ca'),

    -- 네덜란드
    ('University of Amsterdam', 'UvA', 'NL', 'Amsterdam', 52.3667, 4.9000, 'https://www.uva.nl'),
    ('Delft University of Technology', 'TU Delft', 'NL', 'Delft', 51.9986, 4.3731, 'https://www.tudelft.nl'),

    -- 스웨덴
    ('Stockholm University', NULL, 'SE', 'Stockholm', 59.3650, 18.0582, 'https://www.su.se'),
    ('KTH Royal Institute of Technology', 'KTH', 'SE', 'Stockholm', 59.3498, 18.0707, 'https://www.kth.se'),

    -- 싱가포르
    ('National University of Singapore', 'NUS', 'SG', 'Singapore', 1.2966, 103.7764, 'https://www.nus.edu.sg'),
    ('Nanyang Technological University', 'NTU', 'SG', 'Singapore', 1.3483, 103.6831, 'https://www.ntu.edu.sg'),

    -- 홍콩
    ('The University of Hong Kong', 'HKU', 'HK', 'Hong Kong', 22.2835, 114.1363, 'https://www.hku.hk'),
    ('The Hong Kong University of Science and Technology', 'HKUST', 'HK', 'Hong Kong', 22.3363, 114.2635, 'https://www.ust.hk'),

    -- 한국
    ('Seoul National University', '서울대학교', 'KR', 'Seoul', 37.4601, 126.9523, 'https://www.snu.ac.kr'),
    ('Korea University', '고려대학교', 'KR', 'Seoul', 37.5894, 127.0322, 'https://www.korea.ac.kr'),
    ('Yonsei University', '연세대학교', 'KR', 'Seoul', 37.5656, 126.9385, 'https://www.yonsei.ac.kr'),
    ('KAIST', '한국과학기술원', 'KR', 'Daejeon', 36.3725, 127.3600, 'https://www.kaist.ac.kr'),
    ('Sungkyunkwan University', '성균관대학교', 'KR', 'Seoul', 37.5870, 126.9930, 'https://www.skku.edu')
) AS v(name, local_name, country_code, city, latitude, longitude, website_url)
WHERE NOT EXISTS (
    SELECT 1 FROM university u WHERE u.name = v.name
);
