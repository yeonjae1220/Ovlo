-- 개발용 계정 시드 (email: dev@dev.com / password: dev)
INSERT INTO member (name, hometown, email, password, home_university_id, major_name, degree_type, grade_level, status)
SELECT 'dev', 'dev', 'dev@dev.com',
       '$2b$10$G35QWqxUPKNA6K.b0slDL.iB3qL2PUC90k4RSBGRREdXXEkTCPZYe',
       (SELECT id FROM university ORDER BY id LIMIT 1),
       'dev', 'BACHELOR', 1, 'ACTIVE'
WHERE NOT EXISTS (
    SELECT 1 FROM member WHERE email = 'dev@dev.com'
);
