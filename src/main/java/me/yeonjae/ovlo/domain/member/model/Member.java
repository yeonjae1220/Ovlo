package me.yeonjae.ovlo.domain.member.model;

import me.yeonjae.ovlo.domain.university.model.UniversityId;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Member {

    private MemberId id;
    private String name;
    private String hometown;
    private Email email;
    private Password password;
    private UniversityId homeUniversityId;
    private List<UniversityExperience> universityExperiences;
    private List<LanguageSkill> languageSkills;
    private Major major;
    private List<ContactInfo> contactInfos;
    private MemberStatus status;

    // 선택 정보 (추후 수정 가능)
    private String profileImageMediaId;
    private String bio;
    private LocalDate birthDate;

    private Member() {}

    public static Member create(String name, String hometown, Email email, Password password,
                                UniversityId homeUniversityId, Major major) {
        Objects.requireNonNull(name, "이름은 null일 수 없습니다");
        if (name.isBlank()) {
            throw new IllegalArgumentException("이름은 빈 값일 수 없습니다");
        }
        Objects.requireNonNull(hometown, "출신지는 null일 수 없습니다");
        if (hometown.isBlank()) {
            throw new IllegalArgumentException("출신지는 빈 값일 수 없습니다");
        }
        Objects.requireNonNull(email, "이메일은 null일 수 없습니다");
        Objects.requireNonNull(password, "비밀번호는 null일 수 없습니다");
        Objects.requireNonNull(homeUniversityId, "홈 대학은 null일 수 없습니다");
        Objects.requireNonNull(major, "전공 정보는 null일 수 없습니다");

        Member member = new Member();
        member.name = name;
        member.hometown = hometown;
        member.email = email;
        member.password = password;
        member.homeUniversityId = homeUniversityId;
        member.major = major;
        member.universityExperiences = new ArrayList<>();
        member.languageSkills = new ArrayList<>();
        member.contactInfos = new ArrayList<>();
        member.status = MemberStatus.ACTIVE;
        return member;
    }

    // ── 도메인 행위 ──────────────────────────────────────────────────────────

    public void updateProfile(String name, String hometown, Major major) {
        validateActive();
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (hometown != null && !hometown.isBlank()) {
            this.hometown = hometown;
        }
        if (major != null) {
            this.major = major;
        }
    }

    public void addLanguageSkill(LanguageSkill skill) {
        validateActive();
        Objects.requireNonNull(skill, "언어 스킬은 null일 수 없습니다");
        this.languageSkills.add(skill);
    }

    public void addUniversityExperience(UniversityExperience experience) {
        validateActive();
        Objects.requireNonNull(experience, "교류 대학 경험은 null일 수 없습니다");
        this.universityExperiences.add(experience);
    }

    public void updateContactInfos(List<ContactInfo> contactInfos) {
        validateActive();
        Objects.requireNonNull(contactInfos, "연락처 목록은 null일 수 없습니다");
        this.contactInfos = new ArrayList<>(contactInfos);
    }

    public void updateBio(String bio) {
        validateActive();
        this.bio = bio;
    }

    public void updateBirthDate(LocalDate birthDate) {
        validateActive();
        if (birthDate != null && birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("생일은 현재 날짜 이전이어야 합니다");
        }
        this.birthDate = birthDate;
    }

    public void updateProfileImage(String profileImageMediaId) {
        validateActive();
        this.profileImageMediaId = profileImageMediaId;
    }

    public void withdraw() {
        if (status == MemberStatus.WITHDRAWN) {
            throw new IllegalStateException("이미 탈퇴한 회원입니다");
        }
        this.status = MemberStatus.WITHDRAWN;
    }

    // ── private ──────────────────────────────────────────────────────────────

    private void validateActive() {
        if (!status.isActive()) {
            throw new IllegalStateException("활성 회원만 이 작업을 수행할 수 있습니다. 현재 상태: " + status);
        }
    }

    // ── Getters (no setters — 도메인 행위로만 상태 변경) ────────────────────

    public MemberId getId() { return id; }
    public void assignId(MemberId id) { this.id = id; } // persistence 계층 전용

    public String getName() { return name; }
    public String getHometown() { return hometown; }
    public Email getEmail() { return email; }
    public Password getPassword() { return password; }
    public UniversityId getHomeUniversityId() { return homeUniversityId; }
    public Major getMajor() { return major; }
    public MemberStatus getStatus() { return status; }
    public String getBio() { return bio; }
    public LocalDate getBirthDate() { return birthDate; }
    public String getProfileImageMediaId() { return profileImageMediaId; }

    public List<LanguageSkill> getLanguageSkills() {
        return Collections.unmodifiableList(languageSkills);
    }

    public List<UniversityExperience> getUniversityExperiences() {
        return Collections.unmodifiableList(universityExperiences);
    }

    public List<ContactInfo> getContactInfos() {
        return Collections.unmodifiableList(contactInfos);
    }
}
