package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.PostResult;
import me.yeonjae.ovlo.application.port.out.post.LoadPostPort;
import me.yeonjae.ovlo.domain.board.model.BoardId;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.post.exception.PostException;
import me.yeonjae.ovlo.domain.post.model.Post;
import me.yeonjae.ovlo.domain.post.model.PostId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PostQueryServiceTest {

    @Mock LoadPostPort loadPostPort;

    @InjectMocks
    PostQueryService service;

    @Test
    @DisplayName("ID로 게시글을 조회할 수 있다")
    void shouldGetById() {
        PostId postId = new PostId(1L);
        Post post = Post.restore(postId, new BoardId(1L), new MemberId(1L),
                "제목", "내용", false, List.of(), List.of());

        given(loadPostPort.findById(postId)).willReturn(Optional.of(post));

        PostResult result = service.getById(postId);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("제목");
        assertThat(result.likeCount()).isEqualTo(0);
        assertThat(result.comments()).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 ID로 조회하면 예외가 발생한다")
    void shouldThrow_whenNotFound() {
        given(loadPostPort.findById(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(new PostId(999L)))
                .isInstanceOf(PostException.class)
                .hasMessageContaining("게시글을 찾을 수 없습니다");
    }
}
