package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.MemberResult;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.domain.member.exception.MemberException;
import me.yeonjae.ovlo.domain.member.model.*;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MemberQueryServiceTest {

    @Mock
    private LoadMemberPort loadMemberPort;

    @InjectMocks
    private MemberQueryService memberQueryService;

    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.create(
                "yeonjae", "김연재", "Seoul",
                new Email("test@example.com"),
                new Password("hashedPassword"),
                new UniversityId(1L),
                new Major("Computer Science", DegreeType.BACHELOR, 3));
        member.assignId(new MemberId(1L));
    }

    @Nested
    @DisplayName("회원 단건 조회")
    class GetById {

        @Test
        @DisplayName("존재하는 회원을 ID로 조회할 수 있다")
        void shouldGetById_whenExists() {
            given(loadMemberPort.findById(new MemberId(1L))).willReturn(Optional.of(member));

            MemberResult result = memberQueryService.getById(new MemberId(1L));

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.name()).isEqualTo("김연재");
            assertThat(result.email()).isEqualTo("test@example.com");
            assertThat(result.homeUniversityId()).isEqualTo(1L);
            assertThat(result.major().majorName()).isEqualTo("Computer Science");
            assertThat(result.major().degreeType()).isEqualTo("BACHELOR");
            assertThat(result.status()).isEqualTo("ACTIVE");
        }

        @Test
        @DisplayName("존재하지 않는 ID면 예외가 발생한다")
        void shouldThrow_whenNotFound() {
            given(loadMemberPort.findById(new MemberId(999L))).willReturn(Optional.empty());

            assertThatThrownBy(() -> memberQueryService.getById(new MemberId(999L)))
                    .isInstanceOf(MemberException.class)
                    .hasMessageContaining("회원을 찾을 수 없습니다");
        }
    }
}
