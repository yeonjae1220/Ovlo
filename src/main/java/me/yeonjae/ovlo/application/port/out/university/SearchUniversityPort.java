package me.yeonjae.ovlo.application.port.out.university;

import me.yeonjae.ovlo.domain.university.model.University;

import java.util.List;

public interface SearchUniversityPort {
    List<University> search(String keyword, String countryCode, int offset, int limit);
    long count(String keyword, String countryCode);
}
