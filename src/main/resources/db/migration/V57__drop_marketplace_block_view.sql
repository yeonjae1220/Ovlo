-- V56 이 롤링 배포 중 구버전 파드를 위해 남긴 호환 뷰. V56 이 배포된 뒤에는 읽고 쓰는 코드가 없다.
-- IF EXISTS: 뷰가 이미 손으로 지워진 환경에서도 마이그레이션이 멈추지 않게 한다.
DROP VIEW IF EXISTS marketplace_block;
