package me.yeonjae.ovlo.application.port.in.university;

import me.yeonjae.ovlo.application.dto.command.SearchExchangeUniversityCommand;
import me.yeonjae.ovlo.application.dto.result.ExchangeUniversityResult;
import me.yeonjae.ovlo.application.dto.result.PageResult;

public interface SearchExchangeUniversityQuery {
    PageResult<ExchangeUniversityResult> search(SearchExchangeUniversityCommand command);
}
