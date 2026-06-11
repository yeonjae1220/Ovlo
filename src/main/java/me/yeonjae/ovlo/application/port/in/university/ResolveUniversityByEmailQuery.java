package me.yeonjae.ovlo.application.port.in.university;

import me.yeonjae.ovlo.application.dto.result.UniversityEmailResolution;

/**
 * 학교 이메일 주소로 소속 대학을 역조회한다 (학생 인증의 핵심 primitive).
 * 학생 이메일 검증 흐름(코드 발송 전/후)에서 도메인↔대학 일치 여부 판정에 사용.
 */
public interface ResolveUniversityByEmailQuery {
    UniversityEmailResolution resolveByEmail(String email);
}
