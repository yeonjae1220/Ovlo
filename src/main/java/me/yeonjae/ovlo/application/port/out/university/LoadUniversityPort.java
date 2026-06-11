package me.yeonjae.ovlo.application.port.out.university;

import me.yeonjae.ovlo.domain.university.model.University;
import me.yeonjae.ovlo.domain.university.model.UniversityId;

import java.util.Optional;

public interface LoadUniversityPort {
    Optional<University> findById(UniversityId id);

    /** 멤버 본교 등 참조 무결성 검증용. */
    boolean existsById(UniversityId id);
}
