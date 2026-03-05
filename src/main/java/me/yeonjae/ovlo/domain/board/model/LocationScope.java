package me.yeonjae.ovlo.domain.board.model;

public enum LocationScope {
    UNIVERSITY, // 특정 대학 학생만 (universityId 필수)
    REGION,     // 같은 도시/지역
    COUNTRY,    // 같은 국가
    GLOBAL      // 전체 (모든 교환학생)
}
