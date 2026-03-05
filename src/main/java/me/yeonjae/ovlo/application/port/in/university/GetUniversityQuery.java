package me.yeonjae.ovlo.application.port.in.university;

import me.yeonjae.ovlo.application.dto.result.UniversityResult;
import me.yeonjae.ovlo.domain.university.model.UniversityId;

public interface GetUniversityQuery {
    UniversityResult getById(UniversityId id);
}
