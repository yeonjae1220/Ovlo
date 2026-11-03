package me.yeonjae.ovlo.application.port.out.university;

import me.yeonjae.ovlo.domain.university.model.University;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface SearchUniversityPort {
    List<University> search(String keyword, String countryCode, int offset, int limit);
    long count(String keyword, String countryCode);

    /**
     * 표시 언어의 대학명. 그 언어 이름이 없는 대학은 맵에 없다(호출자가 영문으로 폴백).
     *
     * @param lang UI 언어 코드(ko/ja/…). 영문(en)은 name_en 이 곧 표시명이라 호출하지 않아도 된다.
     */
    Map<Long, String> findLocalizedNames(Collection<Long> ids, String lang);
}
