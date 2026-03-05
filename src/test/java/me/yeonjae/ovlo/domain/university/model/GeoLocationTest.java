package me.yeonjae.ovlo.domain.university.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeoLocationTest {

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("유효한 위도·경도로 생성할 수 있다")
        void shouldCreate_whenValid() {
            GeoLocation location = new GeoLocation(37.5665, 126.9780);
            assertThat(location.latitude()).isEqualTo(37.5665);
            assertThat(location.longitude()).isEqualTo(126.9780);
        }

        @Test
        @DisplayName("위도 경계값 -90, 90도 생성할 수 있다")
        void shouldCreate_whenLatitudeAtBoundary() {
            assertThat(new GeoLocation(-90.0, 0.0).latitude()).isEqualTo(-90.0);
            assertThat(new GeoLocation(90.0, 0.0).latitude()).isEqualTo(90.0);
        }

        @Test
        @DisplayName("경도 경계값 -180, 180도 생성할 수 있다")
        void shouldCreate_whenLongitudeAtBoundary() {
            assertThat(new GeoLocation(0.0, -180.0).longitude()).isEqualTo(-180.0);
            assertThat(new GeoLocation(0.0, 180.0).longitude()).isEqualTo(180.0);
        }

        @Test
        @DisplayName("위도가 90을 초과하면 예외가 발생한다")
        void shouldThrow_whenLatitudeExceeds90() {
            assertThatThrownBy(() -> new GeoLocation(90.001, 0.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("위도는 -90 ~ 90 사이여야 합니다");
        }

        @Test
        @DisplayName("위도가 -90 미만이면 예외가 발생한다")
        void shouldThrow_whenLatitudeBelowMinus90() {
            assertThatThrownBy(() -> new GeoLocation(-90.001, 0.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("위도는 -90 ~ 90 사이여야 합니다");
        }

        @Test
        @DisplayName("경도가 180을 초과하면 예외가 발생한다")
        void shouldThrow_whenLongitudeExceeds180() {
            assertThatThrownBy(() -> new GeoLocation(0.0, 180.001))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("경도는 -180 ~ 180 사이여야 합니다");
        }

        @Test
        @DisplayName("경도가 -180 미만이면 예외가 발생한다")
        void shouldThrow_whenLongitudeBelowMinus180() {
            assertThatThrownBy(() -> new GeoLocation(0.0, -180.001))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("경도는 -180 ~ 180 사이여야 합니다");
        }
    }

    @Nested
    @DisplayName("동등성")
    class Equality {

        @Test
        @DisplayName("같은 위도·경도의 GeoLocation은 동일하다")
        void shouldBeEqual_whenSameCoordinates() {
            assertThat(new GeoLocation(37.5665, 126.9780))
                    .isEqualTo(new GeoLocation(37.5665, 126.9780));
        }

        @Test
        @DisplayName("다른 위도의 GeoLocation은 동일하지 않다")
        void shouldNotBeEqual_whenDifferentLatitude() {
            assertThat(new GeoLocation(37.5665, 126.9780))
                    .isNotEqualTo(new GeoLocation(35.6895, 126.9780));
        }
    }
}
