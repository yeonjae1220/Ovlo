package me.yeonjae.ovlo.application.port.in.university;

import me.yeonjae.ovlo.application.dto.command.SearchUniversityCommand;
import me.yeonjae.ovlo.application.dto.result.UniversityPageResult;

public interface SearchUniversityQuery {
    UniversityPageResult search(SearchUniversityCommand command);
}
