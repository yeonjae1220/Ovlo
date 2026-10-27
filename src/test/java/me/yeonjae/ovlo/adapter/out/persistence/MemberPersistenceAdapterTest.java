package me.yeonjae.ovlo.adapter.out.persistence;

import me.yeonjae.ovlo.adapter.out.persistence.repository.MemberJpaRepository;
import me.yeonjae.ovlo.domain.member.model.*;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MemberPersistenceAdapterTest {

    @Autowired
    private MemberPersistenceAdapter adapter;

    @Autowired
    private MemberJpaRepository repository;

    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.create(
                "yeonjae", "김연재", "Seoul",
                new Email("test@example.com"),
                new Password("hashedPassword"),
                new UniversityId(1L),
                new Major("Computer Science", DegreeType.BACHELOR, 3));
    }

    @Nested
    @DisplayName("저장 + 조회")
    class SaveAndFind {

        @Test
        @DisplayName("회원을 저장하고 ID로 조회할 수 있다")
        void shouldSaveAndFindById() {
            Member saved = adapter.save(member);

            Optional<Member> found = adapter.findById(saved.getId());

            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("김연재");
            assertThat(found.get().getEmail().value()).isEqualTo("test@example.com");
            assertThat(found.get().getStatus()).isEqualTo(MemberStatus.ACTIVE);
        }

        @Test
        @DisplayName("회원을 저장하고 이메일로 조회할 수 있다")
        void shouldSaveAndFindByEmail() {
            adapter.save(member);

            Optional<Member> found = adapter.findByEmail("test@example.com");

            assertThat(found).isPresent();
            assertThat(found.get().getName()).isEqualTo("김연재");
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회하면 빈 Optional을 반환한다")
        void shouldReturnEmpty_whenIdNotFound() {
            Optional<Member> found = adapter.findById(new MemberId(999999L));
            assertThat(found).isEmpty();
        }

        @Test
        @DisplayName("이메일 존재 여부를 확인할 수 있다")
        void shouldCheckEmailExists() {
            adapter.save(member);

            assertThat(adapter.existsByEmail("test@example.com")).isTrue();
            assertThat(adapter.existsByEmail("other@example.com")).isFalse();
        }
    }

    @Nested
    @DisplayName("업데이트")
    class Update {

        @Test
        @DisplayName("저장된 회원을 탈퇴 처리할 수 있다")
        void shouldUpdateStatus() {
            Member saved = adapter.save(member);
            saved.withdraw();

            Member updated = adapter.save(saved);

            assertThat(updated.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        }
    }

    @Nested
    @DisplayName("닉네임 검색")
    class SearchByNickname {

        @Test
        @DisplayName("대소문자 무시 부분 일치로 찾되 요청한 개수까지만 닉네임 순으로 돌려준다")
        void shouldReturnLimitedMatchesOrderedByNickname() {
            adapter.save(memberNamed("zqsearch_c"));
            adapter.save(memberNamed("zqsearch_a"));
            adapter.save(memberNamed("zqsearch_b"));
            adapter.save(memberNamed("zqother"));

            List<Member> found = adapter.searchByNickname("ZQSEARCH", 2);

            assertThat(found).extracting(Member::getNickname)
                    .containsExactly("zqsearch_a", "zqsearch_b");
        }

        private Member memberNamed(String nickname) {
            return Member.create(
                    nickname, "검색대상", "Seoul",
                    new Email(nickname + "@example.com"),
                    new Password("hashedPassword"),
                    new UniversityId(1L),
                    new Major("Computer Science", DegreeType.BACHELOR, 3));
        }
    }
}
