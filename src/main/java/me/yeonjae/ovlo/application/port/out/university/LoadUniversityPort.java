package me.yeonjae.ovlo.application.port.out.university;

import me.yeonjae.ovlo.domain.university.model.University;
import me.yeonjae.ovlo.domain.university.model.UniversityId;

import java.util.Optional;

public interface LoadUniversityPort {
    Optional<University> findById(UniversityId id);
}
