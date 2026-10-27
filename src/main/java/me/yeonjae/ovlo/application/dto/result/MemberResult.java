package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberRole;

import java.time.LocalDate;
import java.util.List;

public record MemberResult(
        Long id,
        String nickname,
        String name,
        String hometown,
        String email,
        Long homeUniversityId,
        MajorInfo major,
        String status,
        MemberRole role,
        String bio,
        LocalDate birthDate,
        String profileImageMediaId,
        List<LanguageSkillInfo> languageSkills,
        List<UniversityExperienceInfo> universityExperiences,
        List<ContactInfoData> contactInfos
) {

    public record MajorInfo(String majorName, String degreeType, int gradeLevel) {}

    public record LanguageSkillInfo(String languageCode, String cefrLevel) {}

    public record UniversityExperienceInfo(Long universityId, LocalDate startDate, LocalDate endDate) {}

    public record ContactInfoData(String type, String value) {}

    /**
     * 다른 회원에게 보여줄 사본 — 계정 이메일과 생년월일을 뺀다.
     * 연락처(contactInfos)는 본인이 교류용으로 직접 등록한 것이라 남긴다.
     */
    public MemberResult withoutPrivateFields() {
        return new MemberResult(id, nickname, name, hometown, null, homeUniversityId, major, status, role,
                bio, null, profileImageMediaId, languageSkills, universityExperiences, contactInfos);
    }

    public static MemberResult from(Member member) {
        MajorInfo majorInfo = member.getMajor() != null
                ? new MajorInfo(
                        member.getMajor().majorName(),
                        member.getMajor().degreeType().name(),
                        member.getMajor().gradeLevel())
                : null;

        List<LanguageSkillInfo> skills = member.getLanguageSkills().stream()
                .map(s -> new LanguageSkillInfo(s.languageCode(), s.level().name()))
                .toList();

        List<UniversityExperienceInfo> experiences = member.getUniversityExperiences().stream()
                .map(e -> new UniversityExperienceInfo(
                        e.universityId().value(), e.startDate(), e.endDate()))
                .toList();

        List<ContactInfoData> contacts = member.getContactInfos().stream()
                .map(c -> new ContactInfoData(c.type().name(), c.value()))
                .toList();

        return new MemberResult(
                member.getId() != null ? member.getId().value() : null,
                member.getNickname(),
                member.getName(),
                member.getHometown(),
                member.getEmail().value(),
                member.getHomeUniversityId() != null ? member.getHomeUniversityId().value() : null,
                majorInfo,
                member.getStatus().name(),
                member.getRole(),
                member.getBio(),
                member.getBirthDate(),
                member.getProfileImageMediaId(),
                skills,
                experiences,
                contacts
        );
    }
}
