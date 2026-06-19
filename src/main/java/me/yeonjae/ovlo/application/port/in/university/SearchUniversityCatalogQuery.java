package me.yeonjae.ovlo.application.port.in.university;

import me.yeonjae.ovlo.application.dto.command.SearchUniversityCatalogCommand;
import me.yeonjae.ovlo.application.dto.result.PageResult;
import me.yeonjae.ovlo.application.dto.result.UniversityCatalogCountryResult;
import me.yeonjae.ovlo.application.dto.result.UniversityCatalogResult;

import java.util.List;

/**
 * 콘텐츠(리포트/후기) 보유 대학을 단일 카탈로그로 검색하는 유스케이스.
 */
public interface SearchUniversityCatalogQuery {
    PageResult<UniversityCatalogResult> search(SearchUniversityCatalogCommand command);

    /** 콘텐츠 보유 대학을 국가별로 집계한 목록 (필터 드롭다운용). */
    List<UniversityCatalogCountryResult> getCountries();
}
