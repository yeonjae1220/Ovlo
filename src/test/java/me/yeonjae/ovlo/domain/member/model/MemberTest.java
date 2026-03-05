package me.yeonjae.ovlo.domain.member.model;

import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberTest {

    private Email email;
    private Password password;
    private UniversityId homeUniversityId;
    private Major major;

    @BeforeEach
    void setUp() {
        email = new Email("test@example.com");
        password = new Password("$2a$10$hashedpassword");
        homeUniversityId = new UniversityId(1L);
        major = new Major("Computer Science", DegreeType.BACHELOR, 3);
    }

    @Nested
    @DisplayName("생성")
    class Create {

        @Test
        @DisplayName("필수 정보로 회원을 생성하면 ACTIVE 상태로 시작한다")
        void shouldCreate_withActiveStatus() {
            Member member = Member.create("김연재", "서울", email, password, homeUniversityId, major);

            assertThat(member.getStatus()).isEqualTo(MemberStatus.ACTIVE);
            assertThat(member.getName()).isEqualTo("김연재");
            assertThat(member.getHometown()).isEqualTo("서울");
            assertThat(member.getEmail()).isEqualTo(email);
            assertThat(member.getHomeUniversityId()).isEqualTo(homeUniversityId);
            assertThat(member.getMajor()).isEqualTo(major);
        }

        @Test
        @DisplayName("생성 시 언어 스킬, 교류 대학, 연락처 목록은 비어있다")
        void shouldCreate_withEmptyCollections() {
            Member member = Member.create("김연재", "서울", email, password, homeUniversityId, major);

            assertThat(member.getLanguageSkills()).isEmpty();
            assertThat(member.getUniversityExperiences()).isEmpty();
            assertThat(member.getContactInfos()).isEmpty();
        }

        @Test
        @DisplayName("이름이 null이면 예외가 발생한다")
        void shouldThrow_whenNullName() {
            assertThatThrownBy(() -> Member.create(null, "서울", email, password, homeUniversityId, major))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("이름이 빈 값이면 예외가 발생한다")
        void shouldThrow_whenBlankName() {
            assertThatThrownBy(() -> Member.create("  ", "서울", email, password, homeUniversityId, major))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이름은 빈 값일 수 없습니다");
        }

        @Test
        @DisplayName("홈 대학이 null이면 예외가 발생한다")
        void shouldThrow_whenNullHomeUniversity() {
            assertThatThrownBy(() -> Member.create("김연재", "서울", email, password, null, major))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("프로필 수정")
    class UpdateProfile {

        @Test
        @DisplayName("활성 회원은 이름과 출신지를 수정할 수 있다")
        void shouldUpdateProfile_whenActive() {
            Member member = Member.create("김연재", "서울", email, password, homeUniversityId, major);

            member.updateProfile("이연재", "부산", major);

            assertThat(member.getName()).isEqualTo("이연재");
            assertThat(member.getHometown()).isEqualTo("부산");
        }

        @Test
        @DisplayName("탈퇴한 회원은 프로필을 수정할 수 없다")
        void shouldThrow_whenWithdrawn() {
            Member member = Member.create("김연재", "서울", email, password, homeUniversityId, major);
            member.withdraw();

            assertThatThrownBy(() -> member.updateProfile("이연재", "부산", major))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("활성 회원만");
        }
    }

    @Nested
    @DisplayName("언어 스킬")
    class LanguageSkillManagement {

        @Test
        @DisplayName("언어 스킬을 추가할 수 있다")
        void shouldAddLanguageSkill() {
            Member member = Member.create("김연재", "서울", email, password, homeUniversityId, major);
            LanguageSkill english = new LanguageSkill("en", CefrLevel.B2);

            member.addLanguageSkill(english);

            assertThat(member.getLanguageSkills()).containsExactly(english);
        }

        @Test
        @DisplayName("여러 언어 스킬을 추가할 수 있다")
        void shouldAddMultipleLanguageSkills() {
            Member member = Member.create("김연재", "서울", email, password, homeUniversityId, major);

            member.addLanguageSkill(new LanguageSkill("en", CefrLevel.B2));
            member.addLanguageSkill(new LanguageSkill("fr", CefrLevel.A2));

            assertThat(member.getLanguageSkills()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("교류 대학 경험")
    class UniversityExperienceManagement {

        @Test
        @DisplayName("교류 대학 경험을 추가할 수 있다")
        void shouldAddUniversityExperience() {
            Member member = Member.create("김연재", "서울", email, password, homeUniversityId, major);
            UniversityExperience exp = new UniversityExperience(
                    new UniversityId(2L),
                    LocalDate.of(2024, 9, 1),
                    LocalDate.of(2025, 2, 28)
            );

            member.addUniversityExperience(exp);

            assertThat(member.getUniversityExperiences()).containsExactly(exp);
        }
    }

    @Nested
    @DisplayName("연락처 수정")
    class ContactInfoManagement {

        @Test
        @DisplayName("연락처 목록을 업데이트할 수 있다")
        void shouldUpdateContactInfos() {
            Member member = Member.create("김연재", "서울", email, password, homeUniversityId, major);
            List<ContactInfo> contacts = List.of(
                    new ContactInfo(ContactType.EMAIL, "contact@example.com"),
                    new ContactInfo(ContactType.SNS, "@yeonjae_kim")
            );

            member.updateContactInfos(contacts);

            assertThat(member.getContactInfos()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("탈퇴")
    class Withdraw {

        @Test
        @DisplayName("활성 회원은 탈퇴할 수 있고 WITHDRAWN 상태가 된다")
        void shouldWithdraw_whenActive() {
            Member member = Member.create("김연재", "서울", email, password, homeUniversityId, major);

            member.withdraw();

            assertThat(member.getStatus()).isEqualTo(MemberStatus.WITHDRAWN);
        }

        @Test
        @DisplayName("이미 탈퇴한 회원은 다시 탈퇴할 수 없다")
        void shouldThrow_whenAlreadyWithdrawn() {
            Member member = Member.create("김연재", "서울", email, password, homeUniversityId, major);
            member.withdraw();

            assertThatThrownBy(() -> member.withdraw())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 탈퇴한 회원입니다");
        }
    }

    @Nested
    @DisplayName("자기소개 및 생일")
    class OptionalInfo {

        @Test
        @DisplayName("자기소개를 업데이트할 수 있다")
        void shouldUpdateBio() {
            Member member = Member.create("김연재", "서울", email, password, homeUniversityId, major);

            member.updateBio("교환학생 좋아해요!");

            assertThat(member.getBio()).isEqualTo("교환학생 좋아해요!");
        }

        @Test
        @DisplayName("생일이 미래이면 예외가 발생한다")
        void shouldThrow_whenFutureBirthDate() {
            Member member = Member.create("김연재", "서울", email, password, homeUniversityId, major);

            assertThatThrownBy(() -> member.updateBirthDate(LocalDate.now().plusDays(1)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("생일은 현재 날짜 이전이어야 합니다");
        }
    }
}
