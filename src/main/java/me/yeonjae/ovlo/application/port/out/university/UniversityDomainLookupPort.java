package me.yeonjae.ovlo.application.port.out.university;

import me.yeonjae.ovlo.domain.university.model.University;

import java.util.List;

/**
 * 이메일 도메인으로 대학을 역조회한다 (학교 이메일 검증용).
 * 데이터셋상 동일 도메인을 가진 별개 대학이 존재할 수 있어 List(1:N)를 반환한다.
 */
public interface UniversityDomainLookupPort {
    List<University> findByEmailDomain(String domain);
}
