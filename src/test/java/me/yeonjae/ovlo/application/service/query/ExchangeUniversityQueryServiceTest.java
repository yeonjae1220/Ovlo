package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.command.SearchExchangeUniversityCommand;
import me.yeonjae.ovlo.application.dto.result.ExchangeUniversityResult;
import me.yeonjae.ovlo.application.dto.result.PageResult;
import me.yeonjae.ovlo.application.dto.result.VideoReviewResult;
import me.yeonjae.ovlo.application.port.out.university.LoadExchangeUniversityPort;
import me.yeonjae.ovlo.domain.university.exception.UniversityException;
import me.yeonjae.ovlo.domain.university.model.ExchangeUniversity;
import me.yeonjae.ovlo.domain.university.model.ExchangeUniversityId;
import me.yeonjae.ovlo.domain.university.model.VideoReview;
import me.yeonjae.ovlo.domain.university.model.VideoReviewId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ExchangeUniversityQueryServiceTest {

    @Mock
    private LoadExchangeUniversityPort loadExchangeUniversityPort;

    @InjectMocks
    private ExchangeUniversityQueryService service;

    private ExchangeUniversity tokyo;
    private ExchangeUniversity paris;

    @BeforeEach
    void setUp() {
        tokyo = ExchangeUniversity.restore(1L, "도쿄대학교", "University of Tokyo",
                "일본", "JP", "Tokyo", "https://www.u-tokyo.ac.jp", null);
        paris = ExchangeUniversity.restore(2L, "파리대학교", "Université de Paris",
                "프랑스", "FR", "Paris", "https://u-paris.fr", null);
    }

    @Nested
    @DisplayName("교환 대학 검색")
    class Search {

        @Test
        @DisplayName("키워드로 교환 대학을 검색한다")
        void shouldSearch_byKeyword() {
            given(loadExchangeUniversityPort.search(eq("Tokyo"), isNull(), eq(0), eq(10)))
                    .willReturn(List.of(tokyo));
            given(loadExchangeUniversityPort.count(eq("Tokyo"), isNull())).willReturn(1L);
            given(loadExchangeUniversityPort.countReviewsByUniversityIds(List.of(1L)))
                    .willReturn(Map.of(1L, 5L));
            given(loadExchangeUniversityPort.avgRatingsByUniversityIds(List.of(1L)))
                    .willReturn(Map.of(1L, 4.5));

            PageResult<ExchangeUniversityResult> result = service.search(
                    new SearchExchangeUniversityCommand("Tokyo", null, 0, 10));

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).nameEn()).isEqualTo("University of Tokyo");
            assertThat(result.content().get(0).reviewCount()).isEqualTo(5L);
            assertThat(result.content().get(0).avgRating()).isEqualTo(4.5);
            assertThat(result.totalElements()).isEqualTo(1L);
        }

        @Test
        @DisplayName("국가로 교환 대학을 필터링한다")
        void shouldSearch_byCountry() {
            given(loadExchangeUniversityPort.search(isNull(), eq("FR"), eq(0), eq(10)))
                    .willReturn(List.of(paris));
            given(loadExchangeUniversityPort.count(isNull(), eq("FR"))).willReturn(1L);
            given(loadExchangeUniversityPort.countReviewsByUniversityIds(List.of(2L)))
                    .willReturn(Map.of());
            given(loadExchangeUniversityPort.avgRatingsByUniversityIds(List.of(2L)))
                    .willReturn(Map.of());

            PageResult<ExchangeUniversityResult> result = service.search(
                    new SearchExchangeUniversityCommand(null, "FR", 0, 10));

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).countryCode()).isEqualTo("FR");
        }

        @Test
        @DisplayName("리뷰가 없는 대학은 reviewCount=0, avgRating=null 을 반환한다")
        void shouldReturnZeroReviewCount_whenNoReviews() {
            given(loadExchangeUniversityPort.search(isNull(), isNull(), eq(0), eq(10)))
                    .willReturn(List.of(tokyo));
            given(loadExchangeUniversityPort.count(isNull(), isNull())).willReturn(1L);
            given(loadExchangeUniversityPort.countReviewsByUniversityIds(any())).willReturn(Map.of());
            given(loadExchangeUniversityPort.avgRatingsByUniversityIds(any())).willReturn(Map.of());

            PageResult<ExchangeUniversityResult> result = service.search(
                    new SearchExchangeUniversityCommand(null, null, 0, 10));

            assertThat(result.content().get(0).reviewCount()).isZero();
            assertThat(result.content().get(0).avgRating()).isNull();
        }

        @Test
        @DisplayName("검색 결과가 없으면 빈 목록을 반환한다")
        void shouldReturnEmpty_whenNoResults() {
            given(loadExchangeUniversityPort.search(any(), any(), anyInt(), anyInt()))
                    .willReturn(List.of());
            given(loadExchangeUniversityPort.count(any(), any())).willReturn(0L);

            PageResult<ExchangeUniversityResult> result = service.search(
                    new SearchExchangeUniversityCommand("없는대학", null, 0, 10));

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
            assertThat(result.hasNext()).isFalse();
        }

        @Test
        @DisplayName("페이지 offset이 올바르게 계산된다")
        void shouldCalculateOffset_correctly() {
            given(loadExchangeUniversityPort.search(isNull(), isNull(), eq(20), eq(10)))
                    .willReturn(List.of());
            given(loadExchangeUniversityPort.count(isNull(), isNull())).willReturn(0L);

            service.search(new SearchExchangeUniversityCommand(null, null, 2, 10));

            verify(loadExchangeUniversityPort).search(isNull(), isNull(), eq(20), eq(10));
        }
    }

    @Nested
    @DisplayName("교환 대학 단건 조회")
    class GetById {

        @Test
        @DisplayName("존재하는 ID로 교환 대학을 조회한다")
        void shouldGetById_whenExists() {
            ExchangeUniversityId id = new ExchangeUniversityId(1L);
            given(loadExchangeUniversityPort.findById(id)).willReturn(Optional.of(tokyo));
            given(loadExchangeUniversityPort.countReviewsByUniversityId(id, null)).willReturn(3L);
            given(loadExchangeUniversityPort.avgRatingByUniversityId(id)).willReturn(4.2);

            ExchangeUniversityResult result = service.getById(id);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.nameEn()).isEqualTo("University of Tokyo");
            assertThat(result.nameKo()).isEqualTo("도쿄대학교");
            assertThat(result.countryCode()).isEqualTo("JP");
            assertThat(result.reviewCount()).isEqualTo(3L);
            assertThat(result.avgRating()).isEqualTo(4.2);
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 예외가 발생한다")
        void shouldThrow_whenNotFound() {
            ExchangeUniversityId id = new ExchangeUniversityId(999L);
            given(loadExchangeUniversityPort.findById(id)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(id))
                    .isInstanceOf(UniversityException.class)
                    .hasMessageContaining("대학을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("리뷰 평균이 없는 경우 avgRating은 null이다")
        void shouldReturnNullAvgRating_whenNoReviews() {
            ExchangeUniversityId id = new ExchangeUniversityId(1L);
            given(loadExchangeUniversityPort.findById(id)).willReturn(Optional.of(tokyo));
            given(loadExchangeUniversityPort.countReviewsByUniversityId(id, null)).willReturn(0L);
            given(loadExchangeUniversityPort.avgRatingByUniversityId(id)).willReturn(null);

            ExchangeUniversityResult result = service.getById(id);

            assertThat(result.reviewCount()).isZero();
            assertThat(result.avgRating()).isNull();
        }
    }

    @Nested
    @DisplayName("교환 대학 영상 리뷰 목록")
    class GetReviews {

        @Test
        @DisplayName("존재하는 대학의 리뷰 목록을 페이지네이션으로 조회한다")
        void shouldGetReviews_withPagination() {
            ExchangeUniversityId id = new ExchangeUniversityId(1L);
            VideoReview review = VideoReview.builder()
                    .id(new VideoReviewId(10L))
                    .exchangeUniversityId(id)
                    .youtubeUrl("https://youtube.com/watch?v=abc")
                    .title("도쿄대 교환학생 후기")
                    .overallRating(5)
                    .build();

            given(loadExchangeUniversityPort.findById(id)).willReturn(Optional.of(tokyo));
            given(loadExchangeUniversityPort.findReviewsByUniversityId(id, null, 0, 10))
                    .willReturn(List.of(review));
            given(loadExchangeUniversityPort.countReviewsByUniversityId(id, null)).willReturn(1L);

            PageResult<VideoReviewResult> result = service.getReviews(id, null, 0, 10);

            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).youtubeUrl()).isEqualTo("https://youtube.com/watch?v=abc");
            assertThat(result.content().get(0).title()).isEqualTo("도쿄대 교환학생 후기");
            assertThat(result.totalElements()).isEqualTo(1L);
        }

        @Test
        @DisplayName("존재하지 않는 대학의 리뷰 조회 시 예외가 발생한다")
        void shouldThrow_whenUniversityNotFound() {
            ExchangeUniversityId id = new ExchangeUniversityId(999L);
            given(loadExchangeUniversityPort.findById(id)).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getReviews(id, null, 0, 10))
                    .isInstanceOf(UniversityException.class)
                    .hasMessageContaining("대학을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("리뷰가 없는 대학은 빈 목록을 반환한다")
        void shouldReturnEmpty_whenNoReviews() {
            ExchangeUniversityId id = new ExchangeUniversityId(1L);
            given(loadExchangeUniversityPort.findById(id)).willReturn(Optional.of(tokyo));
            given(loadExchangeUniversityPort.findReviewsByUniversityId(id, null, 0, 10))
                    .willReturn(List.of());
            given(loadExchangeUniversityPort.countReviewsByUniversityId(id, null)).willReturn(0L);

            PageResult<VideoReviewResult> result = service.getReviews(id, null, 0, 10);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
        }
    }
}
