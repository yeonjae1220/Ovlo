package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.MemberResult;
import me.yeonjae.ovlo.application.dto.result.MemberSummaryResult;
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

import java.time.LocalDate;
import java.util.List;
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

    @Nested
    @DisplayName("프로필 조회 — 공개 범위")
    class GetProfile {

        private final MemberId owner = new MemberId(7L);

        private Member memberWithPrivateFields() {
            return Member.restore(owner, "kimchi", "김치", "Seoul",
                    new Email("kimchi@example.com"), new Password("hashed"),
                    OAuthProvider.LOCAL, null,
                    new UniversityId(1L), new Major("CS", DegreeType.BACHELOR, 2),
                    MemberStatus.ACTIVE, null, "안녕하세요", LocalDate.of(2000, 1, 2),
                    List.of(), List.of(),
                    List.of(new ContactInfo(ContactType.SNS, "@kimchi")),
                    MemberRole.MEMBER);
        }

        @Test
        @DisplayName("본인은 계정 이메일·생년월일까지 전부 본다")
        void owner_seesEverything() {
            given(loadMemberPort.findById(owner)).willReturn(Optional.of(memberWithPrivateFields()));

            MemberResult result = memberQueryService.getProfile(owner, owner);

            assertThat(result.email()).isEqualTo("kimchi@example.com");
            assertThat(result.birthDate()).isEqualTo(LocalDate.of(2000, 1, 2));
        }

        @Test
        @DisplayName("다른 회원에게는 계정 이메일·생년월일을 가리고, 본인이 공개용으로 등록한 연락처는 보여준다")
        void others_seeProfileWithoutPrivateFields() {
            given(loadMemberPort.findById(owner)).willReturn(Optional.of(memberWithPrivateFields()));

            MemberResult result = memberQueryService.getProfile(owner, new MemberId(42L));

            assertThat(result.email()).isNull();
            assertThat(result.birthDate()).isNull();
            assertThat(result.nickname()).isEqualTo("kimchi");
            assertThat(result.bio()).isEqualTo("안녕하세요");
            assertThat(result.contactInfos())
                    .extracting(MemberResult.ContactInfoData::value)
                    .containsExactly("@kimchi");
        }

        @Test
        @DisplayName("조회자를 알 수 없으면 다른 회원과 같이 취급한다")
        void unknownViewer_isTreatedAsOthers() {
            given(loadMemberPort.findById(owner)).willReturn(Optional.of(memberWithPrivateFields()));

            MemberResult result = memberQueryService.getProfile(owner, null);

            assertThat(result.email()).isNull();
            assertThat(result.birthDate()).isNull();
        }
    }

    @Nested
    @DisplayName("닉네임 검색")
    class SearchByNickname {

        @Test
        @DisplayName("결과 수 상한을 걸어 조회하고 다른 회원에게 보여도 되는 필드만 담는다")
        void shouldLimitAndReturnSummary() {
            given(loadMemberPort.searchByNickname("yeon", 20)).willReturn(List.of(member));

            List<MemberSummaryResult> results = memberQueryService.searchByNickname("yeon");

            assertThat(results).containsExactly(new MemberSummaryResult(1L, "yeonjae", "김연재", null));
        }
    }

    @Nested
    @DisplayName("닉네임 사용 가능 여부")
    class NicknameAvailability {

        @Test
        @DisplayName("같은 닉네임의 회원이 없으면 사용할 수 있다")
        void shouldBeAvailable_whenNicknameIsFree() {
            given(loadMemberPort.existsByNickname("newbie")).willReturn(false);

            assertThat(memberQueryService.isNicknameAvailable("newbie")).isTrue();
        }

        @Test
        @DisplayName("같은 닉네임의 회원이 있으면 사용할 수 없다")
        void shouldBeUnavailable_whenNicknameIsTaken() {
            given(loadMemberPort.existsByNickname("yeonjae")).willReturn(true);

            assertThat(memberQueryService.isNicknameAvailable("yeonjae")).isFalse();
        }
    }
}
