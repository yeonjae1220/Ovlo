package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.command.SearchUniversityCatalogCommand;
import me.yeonjae.ovlo.application.dto.result.PageResult;
import me.yeonjae.ovlo.application.dto.result.UniversityCatalogCountryResult;
import me.yeonjae.ovlo.application.dto.result.UniversityCatalogResult;
import me.yeonjae.ovlo.application.port.out.university.LoadExchangeUniversityPort;
import me.yeonjae.ovlo.application.port.out.university.LoadUniversityCatalogPort;
import me.yeonjae.ovlo.application.port.out.university.LoadUniversityCatalogPort.CatalogRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UniversityCatalogQueryServiceTest {

    @Mock LoadUniversityCatalogPort loadCatalogPort;
    @Mock LoadExchangeUniversityPort loadExchangePort;

    @InjectMocks
    UniversityCatalogQueryService service;

    @Test
    @DisplayName("리포트만 보유한 대학은 hasReport=true, hasReviews=false 로 매핑된다")
    void shouldMap_reportOnly() {
        CatalogRow row = new CatalogRow(10L, null, 100L,
                "Seoul National University", "서울대학교", "대한민국", "KR", "Seoul");
        given(loadCatalogPort.search(eq("서울"), isNull(), eq(0), eq(20))).willReturn(List.of(row));
        given(loadCatalogPort.count(eq("서울"), isNull())).willReturn(1L);
        // exchangeUnivId 가 없으므로 빈 리스트로 집계 조회
        given(loadExchangePort.countReviewsByUniversityIds(eq(List.of()))).willReturn(Map.of());
        given(loadExchangePort.avgRatingsByUniversityIds(eq(List.of()))).willReturn(Map.of());

        PageResult<UniversityCatalogResult> page =
                service.search(new SearchUniversityCatalogCommand("서울", null, 0, 20));

        assertThat(page.content()).hasSize(1);
        UniversityCatalogResult r = page.content().get(0);
        assertThat(r.globalUnivId()).isEqualTo(10L);
        assertThat(r.reportId()).isEqualTo(100L);
        assertThat(r.hasReport()).isTrue();
        assertThat(r.hasReviews()).isFalse();
        assertThat(r.reviewCount()).isZero();
        assertThat(r.avgRating()).isNull();
        assertThat(r.nameKo()).isEqualTo("서울대학교");
    }

    @Test
    @DisplayName("후기만 보유한 대학은 hasReviews=true 이고 리뷰 집계가 채워진다")
    void shouldMap_reviewsOnly() {
        CatalogRow row = new CatalogRow(20L, 200L, null,
                "Tokyo University", "도쿄대학교", "일본", "JP", "Tokyo");
        given(loadCatalogPort.search(isNull(), eq("JP"), eq(0), eq(20))).willReturn(List.of(row));
        given(loadCatalogPort.count(isNull(), eq("JP"))).willReturn(1L);
        given(loadExchangePort.countReviewsByUniversityIds(eq(List.of(200L)))).willReturn(Map.of(200L, 7L));
        given(loadExchangePort.avgRatingsByUniversityIds(eq(List.of(200L)))).willReturn(Map.of(200L, 4.5));

        PageResult<UniversityCatalogResult> page =
                service.search(new SearchUniversityCatalogCommand(null, "JP", 0, 20));

        UniversityCatalogResult r = page.content().get(0);
        assertThat(r.exchangeUnivId()).isEqualTo(200L);
        assertThat(r.hasReport()).isFalse();
        assertThat(r.hasReviews()).isTrue();
        assertThat(r.reviewCount()).isEqualTo(7L);
        assertThat(r.avgRating()).isEqualTo(4.5);
    }

    @Test
    @DisplayName("리포트와 후기를 모두 보유하면 두 플래그가 모두 true 다")
    void shouldMap_both() {
        CatalogRow row = new CatalogRow(30L, 300L, 301L,
                "ETH Zurich", "취리히공대", "스위스", "CH", "Zurich");
        given(loadCatalogPort.search(isNull(), isNull(), eq(0), eq(20))).willReturn(List.of(row));
        given(loadCatalogPort.count(isNull(), isNull())).willReturn(1L);
        given(loadExchangePort.countReviewsByUniversityIds(eq(List.of(300L)))).willReturn(Map.of(300L, 2L));
        given(loadExchangePort.avgRatingsByUniversityIds(eq(List.of(300L)))).willReturn(Map.of(300L, 3.0));

        PageResult<UniversityCatalogResult> page =
                service.search(new SearchUniversityCatalogCommand(null, null, 0, 20));

        UniversityCatalogResult r = page.content().get(0);
        assertThat(r.hasReport()).isTrue();
        assertThat(r.hasReviews()).isTrue();
        assertThat(r.reportId()).isEqualTo(301L);
        assertThat(r.reviewCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getCountries는 포트의 국가 집계 결과를 그대로 전달한다")
    void shouldDelegateCountries() {
        given(loadCatalogPort.findCountries()).willReturn(List.of(
                new UniversityCatalogCountryResult("대한민국", "KR", 3L),
                new UniversityCatalogCountryResult("독일", "DE", 1L)));

        List<UniversityCatalogCountryResult> countries = service.getCountries();

        assertThat(countries).extracting(UniversityCatalogCountryResult::countryCode)
                .containsExactly("KR", "DE");
        assertThat(countries.get(0).universityCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("페이징 메타데이터(totalPages/hasNext)가 올바르게 계산된다")
    void shouldComputePaging() {
        given(loadCatalogPort.search(isNull(), isNull(), eq(0), eq(20))).willReturn(List.of());
        given(loadCatalogPort.count(isNull(), isNull())).willReturn(45L);
        given(loadExchangePort.countReviewsByUniversityIds(eq(List.of()))).willReturn(Map.of());
        given(loadExchangePort.avgRatingsByUniversityIds(eq(List.of()))).willReturn(Map.of());

        PageResult<UniversityCatalogResult> page =
                service.search(new SearchUniversityCatalogCommand(null, null, 0, 20));

        assertThat(page.totalElements()).isEqualTo(45L);
        assertThat(page.totalPages()).isEqualTo(3);
        assertThat(page.hasNext()).isTrue();
    }
}
