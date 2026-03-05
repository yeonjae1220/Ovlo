package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.command.SearchUniversityCommand;
import me.yeonjae.ovlo.application.dto.result.UniversityPageResult;
import me.yeonjae.ovlo.application.dto.result.UniversityResult;
import me.yeonjae.ovlo.application.port.out.university.LoadUniversityPort;
import me.yeonjae.ovlo.application.port.out.university.SearchUniversityPort;
import me.yeonjae.ovlo.domain.university.exception.UniversityException;
import me.yeonjae.ovlo.domain.university.model.CountryCode;
import me.yeonjae.ovlo.domain.university.model.GeoLocation;
import me.yeonjae.ovlo.domain.university.model.University;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.eq;

@ExtendWith(MockitoExtension.class)
class UniversityQueryServiceTest {

    @Mock
    private LoadUniversityPort loadUniversityPort;

    @Mock
    private SearchUniversityPort searchUniversityPort;

    @InjectMocks
    private UniversityQueryService universityQueryService;

    private University snu;
    private University yonsei;

    @BeforeEach
    void setUp() {
        snu = University.restore(
                new UniversityId(1L),
                "Seoul National University",
                "서울대학교",
                new CountryCode("KR"),
                "Seoul",
                new GeoLocation(37.4600, 126.9514),
                "https://www.snu.ac.kr");

        yonsei = University.restore(
                new UniversityId(2L),
                "Yonsei University",
                "연세대학교",
                new CountryCode("KR"),
                "Seoul",
                new GeoLocation(37.5665, 126.9390),
                "https://www.yonsei.ac.kr");
    }

    @Nested
    @DisplayName("대학 검색")
    class Search {

        @Test
        @DisplayName("키워드와 국가코드로 대학을 검색한다")
        void shouldSearch_withKeywordAndCountry() {
            given(searchUniversityPort.search("Seoul", "KR", 0, 10)).willReturn(List.of(snu));
            given(searchUniversityPort.count("Seoul", "KR")).willReturn(1L);

            UniversityPageResult result = universityQueryService.search(
                    new SearchUniversityCommand("Seoul", "KR", 0, 10));

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).name()).isEqualTo("Seoul National University");
            assertThat(result.totalElements()).isEqualTo(1L);
        }

        @Test
        @DisplayName("결과가 없으면 빈 목록을 반환한다")
        void shouldReturnEmpty_whenNoResults() {
            given(searchUniversityPort.search(anyString(), isNull(), anyInt(), anyInt())).willReturn(List.of());
            given(searchUniversityPort.count(anyString(), isNull())).willReturn(0L);

            UniversityPageResult result = universityQueryService.search(
                    new SearchUniversityCommand("nonexistent", null, 0, 10));

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }

        @Test
        @DisplayName("페이징이 적용된다")
        void shouldApplyPaging() {
            given(searchUniversityPort.search(isNull(), isNull(), eq(10), eq(10))).willReturn(List.of(yonsei));
            given(searchUniversityPort.count(isNull(), isNull())).willReturn(15L);

            UniversityPageResult result = universityQueryService.search(
                    new SearchUniversityCommand(null, null, 1, 10));

            assertThat(result.content()).hasSize(1);
            assertThat(result.page()).isEqualTo(1);
            assertThat(result.size()).isEqualTo(10);
            assertThat(result.totalElements()).isEqualTo(15L);
        }
    }

    @Nested
    @DisplayName("대학 단건 조회")
    class GetById {

        @Test
        @DisplayName("존재하는 대학 ID로 조회할 수 있다")
        void shouldGetById_whenExists() {
            given(loadUniversityPort.findById(new UniversityId(1L))).willReturn(Optional.of(snu));

            UniversityResult result = universityQueryService.getById(new UniversityId(1L));

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("Seoul National University");
            assertThat(result.localName()).isEqualTo("서울대학교");
            assertThat(result.countryCode()).isEqualTo("KR");
            assertThat(result.city()).isEqualTo("Seoul");
            assertThat(result.latitude()).isEqualTo(37.4600);
            assertThat(result.longitude()).isEqualTo(126.9514);
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 예외가 발생한다")
        void shouldThrow_whenNotFound() {
            given(loadUniversityPort.findById(new UniversityId(999L))).willReturn(Optional.empty());

            assertThatThrownBy(() -> universityQueryService.getById(new UniversityId(999L)))
                    .isInstanceOf(UniversityException.class)
                    .hasMessageContaining("대학을 찾을 수 없습니다");
        }
    }
}
