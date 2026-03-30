package me.yeonjae.ovlo.application.port.in.university;

import me.yeonjae.ovlo.application.dto.command.SearchGlobalUniversityCommand;
import me.yeonjae.ovlo.application.dto.result.GlobalUniversityResult;
import me.yeonjae.ovlo.application.dto.result.PageResult;

public interface SearchGlobalUniversityQuery {
    PageResult<GlobalUniversityResult> search(SearchGlobalUniversityCommand command);
}
