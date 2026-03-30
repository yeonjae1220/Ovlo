package me.yeonjae.ovlo.application.port.out.university;

import me.yeonjae.ovlo.domain.university.model.GlobalUniversity;

import java.util.List;

public interface LoadGlobalUniversityPort {
    List<GlobalUniversity> search(String keyword, String countryCode, int offset, int limit);
    long count(String keyword, String countryCode);
}
