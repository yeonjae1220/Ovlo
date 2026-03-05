package me.yeonjae.ovlo.application.dto.result;

import me.yeonjae.ovlo.domain.member.model.Member;

import java.time.LocalDate;
import java.util.List;

public record MemberResult(
        Long id,
        String name,
        String hometown,
        String email,
        Long homeUniversityId,
        MajorInfo major,
        String status,
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

    public static MemberResult from(Member member) {
        MajorInfo majorInfo = new MajorInfo(
                member.getMajor().majorName(),
                member.getMajor().degreeType().name(),
                member.getMajor().gradeLevel()
        );

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
                member.getName(),
                member.getHometown(),
                member.getEmail().value(),
                member.getHomeUniversityId().value(),
                majorInfo,
                member.getStatus().name(),
                member.getBio(),
                member.getBirthDate(),
                member.getProfileImageMediaId(),
                skills,
                experiences,
                contacts
        );
    }
}
