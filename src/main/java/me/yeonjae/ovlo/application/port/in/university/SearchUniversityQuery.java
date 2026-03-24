package me.yeonjae.ovlo.application.port.in.university;

import me.yeonjae.ovlo.application.dto.command.SearchUniversityCommand;
import me.yeonjae.ovlo.application.dto.result.PageResult;
import me.yeonjae.ovlo.application.dto.result.UniversityResult;

public interface SearchUniversityQuery {
    PageResult<UniversityResult> search(SearchUniversityCommand command);
}
