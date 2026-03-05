package me.yeonjae.ovlo.domain.university.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UniversityTest {

    private CountryCode korea;
    private GeoLocation seoul;

    @BeforeEach
    void setUp() {
        korea = new CountryCode("KR");
        seoul = new GeoLocation(37.5665, 126.9780);
    }

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("필수 항목으로 대학을 생성할 수 있다")
        void shouldCreate_withRequiredFields() {
            University university = University.create("Seoul National University", null, korea, "Seoul", seoul, null);

            assertThat(university.getName()).isEqualTo("Seoul National University");
            assertThat(university.getLocalName()).isNull();
            assertThat(university.getCountryCode()).isEqualTo(korea);
            assertThat(university.getCity()).isEqualTo("Seoul");
            assertThat(university.getGeoLocation()).isEqualTo(seoul);
            assertThat(university.getWebsiteUrl()).isNull();
            assertThat(university.getId()).isNull();
        }

        @Test
        @DisplayName("선택 항목까지 포함하여 대학을 생성할 수 있다")
        void shouldCreate_withOptionalFields() {
            University university = University.create(
                    "Seoul National University",
                    "서울대학교",
                    korea,
                    "Seoul",
                    seoul,
                    "https://www.snu.ac.kr");

            assertThat(university.getLocalName()).isEqualTo("서울대학교");
            assertThat(university.getWebsiteUrl()).isEqualTo("https://www.snu.ac.kr");
        }

        @Test
        @DisplayName("이름이 null이면 예외가 발생한다")
        void shouldThrow_whenNameIsNull() {
            assertThatThrownBy(() -> University.create(null, null, korea, "Seoul", seoul, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("대학명은 필수입니다");
        }

        @Test
        @DisplayName("이름이 빈 값이면 예외가 발생한다")
        void shouldThrow_whenNameIsBlank() {
            assertThatThrownBy(() -> University.create("  ", null, korea, "Seoul", seoul, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("대학명은 빈 값일 수 없습니다");
        }

        @Test
        @DisplayName("국가 코드가 null이면 예외가 발생한다")
        void shouldThrow_whenCountryCodeIsNull() {
            assertThatThrownBy(() -> University.create("SNU", null, null, "Seoul", seoul, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("국가 코드는 필수입니다");
        }

        @Test
        @DisplayName("도시가 null이면 예외가 발생한다")
        void shouldThrow_whenCityIsNull() {
            assertThatThrownBy(() -> University.create("SNU", null, korea, null, seoul, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("도시는 필수입니다");
        }

        @Test
        @DisplayName("도시가 빈 값이면 예외가 발생한다")
        void shouldThrow_whenCityIsBlank() {
            assertThatThrownBy(() -> University.create("SNU", null, korea, "  ", seoul, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("도시는 빈 값일 수 없습니다");
        }

        @Test
        @DisplayName("좌표가 null이면 예외가 발생한다")
        void shouldThrow_whenGeoLocationIsNull() {
            assertThatThrownBy(() -> University.create("SNU", null, korea, "Seoul", null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("좌표는 필수입니다");
        }
    }

    @Nested
    @DisplayName("도메인 행위")
    class Behavior {

        private University university;

        @BeforeEach
        void setUp() {
            university = University.create("Seoul National University", null, korea, "Seoul", seoul, null);
        }

        @Test
        @DisplayName("좌표를 변경할 수 있다")
        void shouldUpdateGeoLocation() {
            GeoLocation newLocation = new GeoLocation(37.4600, 126.9514);

            university.updateGeoLocation(newLocation);

            assertThat(university.getGeoLocation()).isEqualTo(newLocation);
        }

        @Test
        @DisplayName("좌표를 null로 변경하면 예외가 발생한다")
        void shouldThrow_whenUpdatingGeoLocationToNull() {
            assertThatThrownBy(() -> university.updateGeoLocation(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("좌표는 필수입니다");
        }

        @Test
        @DisplayName("웹사이트 URL을 변경할 수 있다")
        void shouldUpdateWebsiteUrl() {
            university.updateWebsiteUrl("https://www.snu.ac.kr");

            assertThat(university.getWebsiteUrl()).isEqualTo("https://www.snu.ac.kr");
        }

        @Test
        @DisplayName("웹사이트 URL을 null로 초기화할 수 있다")
        void shouldClearWebsiteUrl() {
            University uni = University.create("SNU", null, korea, "Seoul", seoul, "https://snu.ac.kr");
            uni.updateWebsiteUrl(null);

            assertThat(uni.getWebsiteUrl()).isNull();
        }
    }

    @Nested
    @DisplayName("저장소 복원")
    class Restore {

        @Test
        @DisplayName("ID와 함께 복원할 수 있다")
        void shouldRestore_withId() {
            UniversityId id = new UniversityId(1L);
            University university = University.restore(id, "SNU", null, korea, "Seoul", seoul, null);

            assertThat(university.getId()).isEqualTo(id);
            assertThat(university.getName()).isEqualTo("SNU");
        }
    }
}
