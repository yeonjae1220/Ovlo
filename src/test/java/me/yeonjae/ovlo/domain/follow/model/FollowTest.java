package me.yeonjae.ovlo.domain.follow.model;

import me.yeonjae.ovlo.domain.member.model.MemberId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FollowTest {

    private final MemberId followerId = new MemberId(1L);
    private final MemberId followeeId = new MemberId(2L);

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("팔로우 관계를 생성할 수 있다")
        void shouldCreate_follow() {
            Follow follow = Follow.create(followerId, followeeId);

            assertThat(follow.getFollowerId()).isEqualTo(followerId);
            assertThat(follow.getFolloweeId()).isEqualTo(followeeId);
            assertThat(follow.getId()).isNull();
        }

        @Test
        @DisplayName("followerId가 null이면 예외가 발생한다")
        void shouldThrow_whenFollowerIdIsNull() {
            assertThatThrownBy(() -> Follow.create(null, followeeId))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("팔로워 ID는 필수입니다");
        }

        @Test
        @DisplayName("followeeId가 null이면 예외가 발생한다")
        void shouldThrow_whenFolloweeIdIsNull() {
            assertThatThrownBy(() -> Follow.create(followerId, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("팔로이 ID는 필수입니다");
        }

        @Test
        @DisplayName("자기 자신을 팔로우하면 예외가 발생한다")
        void shouldThrow_whenFollowingSelf() {
            MemberId selfId = new MemberId(1L);

            assertThatThrownBy(() -> Follow.create(selfId, selfId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("자기 자신을 팔로우할 수 없습니다");
        }
    }

    @Nested
    @DisplayName("restore()")
    class Restore {

        @Test
        @DisplayName("팔로우 관계를 복원할 수 있다")
        void shouldRestore_follow() {
            FollowId followId = new FollowId(10L);

            Follow follow = Follow.restore(followId, followerId, followeeId);

            assertThat(follow.getId()).isEqualTo(followId);
            assertThat(follow.getFollowerId()).isEqualTo(followerId);
            assertThat(follow.getFolloweeId()).isEqualTo(followeeId);
        }
    }
}
