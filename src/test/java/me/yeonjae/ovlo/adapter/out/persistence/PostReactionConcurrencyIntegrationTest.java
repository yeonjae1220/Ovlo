package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.application.dto.command.ReactToPostCommand;
import me.yeonjae.ovlo.application.dto.command.UnreactToPostCommand;
import me.yeonjae.ovlo.application.port.in.post.ReactToPostUseCase;
import me.yeonjae.ovlo.domain.post.exception.PostException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 좋아요(반응) 동시성 통합 테스트 — 실 PostgreSQL(Testcontainers) + Flyway.
 *
 * <p><b>A안</b>: 반응은 회원 1명당 {@code post_reaction} 1행({@code (post_id, member_id)} PK)으로
 * idempotent 하게 관리된다. 서로 다른 회원의 동시 좋아요는 서로 다른 행을 건드리므로 애초에 충돌하지
 * 않고, 유일하게 공유되는 비정규화 카운트는 {@code UPDATE post SET like_count = like_count + :delta}
 * 원자 증감으로 갱신한다. 따라서 @Version 낙관적 락이나 재시도 데코레이터 없이도 동시 좋아요가 모두
 * 보존되고 카운트가 정확히 수렴해야 한다.
 */
@Tag("integration")
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class PostReactionConcurrencyIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.properties.hibernate.dialect",
                () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired
    private ReactToPostUseCase reactToPostUseCase;

    @Autowired
    private JdbcTemplate jdbc;

    private Long postId;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM post_reaction");
        jdbc.update("DELETE FROM post");
        postId = jdbc.queryForObject(
                "INSERT INTO post (board_id, author_id, title, content, deleted, hidden_by_withdrawal, version) " +
                        "VALUES (1, 1, '제목', '내용', false, false, 0) RETURNING id",
                Long.class);
    }

    @Test
    @DisplayName("같은 게시글에 대한 다수 회원의 동시 좋아요가 모두 보존되고 카운트가 정확히 수렴한다 (락 없는 회원별 upsert + 원자 증감)")
    void concurrentLikesByDistinctMembersAreAllPersisted() throws Exception {
        int rounds = 12;
        int concurrency = 4; // 한 라운드에 4명이 같은 게시글에 동시 좋아요 → 카운트 컬럼에서만 짧게 직렬화(실패·재시도 없음)
        long memberSeq = 1000L;

        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        try {
            for (int round = 0; round < rounds; round++) {
                CyclicBarrier barrier = new CyclicBarrier(concurrency);
                List<Future<?>> futures = new ArrayList<>();
                for (int i = 0; i < concurrency; i++) {
                    long memberId = memberSeq++;
                    futures.add(pool.submit(() -> {
                        barrier.await(); // 전원 동시 출발 → 경합 최대화
                        reactToPostUseCase.react(new ReactToPostCommand(postId, memberId, "LIKE"));
                        return null;
                    }));
                }
                for (Future<?> f : futures) {
                    f.get();
                }
            }
        } finally {
            pool.shutdownNow();
        }

        long expected = (long) rounds * concurrency;

        Long reactionRows = jdbc.queryForObject(
                "SELECT count(*) FROM post_reaction WHERE post_id = ?", Long.class, postId);
        assertThat(reactionRows)
                .as("동시 좋아요 %d건의 반응 행이 모두 보존되어야 함 (lost update 없음)", expected)
                .isEqualTo(expected);

        Long likeCount = jdbc.queryForObject(
                "SELECT like_count FROM post WHERE id = ?", Long.class, postId);
        assertThat(likeCount)
                .as("비정규화 like_count 가 원자 증감으로 정확히 %d 로 수렴해야 함", expected)
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("좋아요→싫어요 전환·취소 시 비정규화 카운트가 정확히 조정된다")
    void toggleAndUnreactAdjustCountsCorrectly() {
        // 좋아요 3, 싫어요 1
        reactToPostUseCase.react(new ReactToPostCommand(postId, 1L, "LIKE"));
        reactToPostUseCase.react(new ReactToPostCommand(postId, 2L, "LIKE"));
        reactToPostUseCase.react(new ReactToPostCommand(postId, 3L, "LIKE"));
        reactToPostUseCase.react(new ReactToPostCommand(postId, 4L, "DISLIKE"));
        assertThat(likeCount()).isEqualTo(3);
        assertThat(dislikeCount()).isEqualTo(1);

        // 회원 1: 좋아요 → 싫어요 전환 (like -1, dislike +1)
        reactToPostUseCase.react(new ReactToPostCommand(postId, 1L, "DISLIKE"));
        assertThat(likeCount()).isEqualTo(2);
        assertThat(dislikeCount()).isEqualTo(2);

        // 회원 2: 좋아요 취소 (like -1)
        reactToPostUseCase.unreact(new UnreactToPostCommand(postId, 2L));
        assertThat(likeCount()).isEqualTo(1);
        assertThat(dislikeCount()).isEqualTo(2);

        // 카운트와 실제 반응 행 수가 일치해야 함
        assertThat(reactionRowCount("LIKE")).isEqualTo(1);
        assertThat(reactionRowCount("DISLIKE")).isEqualTo(2);
    }

    @Test
    @DisplayName("같은 회원의 동시 중복 좋아요는 정확히 하나만 성공하고 나머지는 409(CONFLICT)로 수렴한다 (중복 카운트 없음)")
    void concurrentDuplicateLikesBySameMemberConvergeToOne() throws Exception {
        int concurrency = 4; // 한 회원이 같은 게시글에 4번 동시 좋아요 (더블클릭/중복 제출)
        long memberId = 2000L;

        ExecutorService pool = Executors.newFixedThreadPool(concurrency);
        CyclicBarrier barrier = new CyclicBarrier(concurrency);
        List<Future<Throwable>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < concurrency; i++) {
                futures.add(pool.submit(() -> {
                    barrier.await(); // 전원 동시 출발
                    try {
                        reactToPostUseCase.react(new ReactToPostCommand(postId, memberId, "LIKE"));
                        return null; // 성공
                    } catch (Throwable t) {
                        return t; // 실패 원인 반환(예외 유형 검증용)
                    }
                }));
            }

            int success = 0;
            List<Throwable> failures = new ArrayList<>();
            for (Future<Throwable> f : futures) {
                Throwable t = f.get();
                if (t == null) success++;
                else failures.add(t);
            }

            assertThat(success)
                    .as("동시 중복 좋아요 중 정확히 하나만 성공해야 함")
                    .isEqualTo(1);
            assertThat(failures)
                    .as("나머지 %d건은 모두 실패해야 함", concurrency - 1)
                    .hasSize(concurrency - 1);
            // 진 트랜잭션의 예외는 모두 409(CONFLICT)로 매핑되는 유형이어야 함:
            //  - 로드 후 경쟁 삽입 → PK 위반 DataIntegrityViolationException (→ 409)
            //  - 선행 커밋 후 로드 → 도메인 CONFLICT PostException (→ 409)
            assertThat(failures).allSatisfy(t -> assertThat(is409Mappable(t))
                    .as("실패 예외는 409(CONFLICT)로 매핑되어야 함: %s", t)
                    .isTrue());
        } finally {
            pool.shutdownNow();
        }

        // 데이터 정합: PK 무결성으로 반응은 1행, like_count 는 정확히 1 (중복 증가 없음)
        assertThat(reactionRowCount("LIKE")).as("반응 행은 정확히 1개").isEqualTo(1);
        assertThat(likeCount()).as("like_count 는 정확히 1 (중복 증가 없음)").isEqualTo(1);
    }

    /** 409(CONFLICT)로 매핑되는 실패인지 — PK 위반(DataIntegrityViolation) 또는 도메인 CONFLICT. */
    private static boolean is409Mappable(Throwable t) {
        if (t instanceof DataIntegrityViolationException) {
            return true;
        }
        return t instanceof PostException pe
                && pe.getErrorType() == PostException.ErrorType.CONFLICT;
    }

    private long likeCount() {
        return jdbc.queryForObject("SELECT like_count FROM post WHERE id = ?", Long.class, postId);
    }

    private long dislikeCount() {
        return jdbc.queryForObject("SELECT dislike_count FROM post WHERE id = ?", Long.class, postId);
    }

    private long reactionRowCount(String type) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM post_reaction WHERE post_id = ? AND reaction_type = ?",
                Long.class, postId, type);
    }
}
