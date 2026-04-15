package me.yeonjae.ovlo.adapter.out.persistence.mapper;

import me.yeonjae.ovlo.adapter.out.persistence.entity.*;
import me.yeonjae.ovlo.domain.member.model.*;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MemberMapper {

    public MemberJpaEntity toJpaEntity(Member member) {
        MemberJpaEntity entity = new MemberJpaEntity();
        if (member.getId() != null) {
            entity.setId(member.getId().value());
        }
        entity.setNickname(member.getNickname());
        entity.setName(member.getName());
        entity.setHometown(member.getHometown());
        entity.setEmail(member.getEmail().value());
        entity.setPassword(member.getPassword() != null ? member.getPassword().hashedValue() : null);
        entity.setProvider(member.getProvider() != null ? member.getProvider() : me.yeonjae.ovlo.domain.member.model.OAuthProvider.LOCAL);
        entity.setProviderId(member.getProviderId());
        entity.setHomeUniversityId(member.getHomeUniversityId() != null ? member.getHomeUniversityId().value() : null);
        entity.setProfileImageMediaId(member.getProfileImageMediaId());
        entity.setBio(member.getBio());
        entity.setBirthDate(member.getBirthDate());
        entity.setStatus(member.getStatus());
        entity.setRole(member.getRole());
        entity.setMajorName(member.getMajor() != null ? member.getMajor().majorName() : null);
        entity.setDegreeType(member.getMajor() != null ? member.getMajor().degreeType() : null);
        entity.setGradeLevel(member.getMajor() != null ? member.getMajor().gradeLevel() : null);

        entity.setLanguageSkills(member.getLanguageSkills().stream()
                .map(s -> new LanguageSkillEmbeddable(s.languageCode(), s.level()))
                .toList());
        entity.setUniversityExperiences(member.getUniversityExperiences().stream()
                .map(e -> new UniversityExperienceEmbeddable(
                        e.universityId().value(), e.startDate(), e.endDate()))
                .toList());
        entity.setContactInfos(member.getContactInfos().stream()
                .map(c -> new ContactInfoEmbeddable(c.type(), c.value()))
                .toList());

        return entity;
    }

    public Member toDomain(MemberJpaEntity entity) {
        List<LanguageSkill> skills = entity.getLanguageSkills().stream()
                .map(s -> new LanguageSkill(s.getLanguageCode(), s.getCefrLevel()))
                .toList();

        List<UniversityExperience> experiences = entity.getUniversityExperiences().stream()
                .map(e -> new UniversityExperience(
                        new UniversityId(e.getUniversityId()), e.getStartDate(), e.getEndDate()))
                .toList();

        List<ContactInfo> contacts = entity.getContactInfos().stream()
                .map(c -> new ContactInfo(c.getContactType(), c.getValue()))
                .toList();

        UniversityId homeUniversityId = entity.getHomeUniversityId() != null
                ? new UniversityId(entity.getHomeUniversityId()) : null;
        Major major = (entity.getMajorName() != null && entity.getDegreeType() != null && entity.getGradeLevel() != null)
                ? new Major(entity.getMajorName(), entity.getDegreeType(), entity.getGradeLevel()) : null;

        return Member.restore(
                new MemberId(entity.getId()),
                entity.getNickname(),
                entity.getName(),
                entity.getHometown(),
                new Email(entity.getEmail()),
                entity.getPassword() != null ? new Password(entity.getPassword()) : null,
                entity.getProvider(),
                entity.getProviderId(),
                homeUniversityId,
                major,
                entity.getStatus(),
                entity.getProfileImageMediaId(),
                entity.getBio(),
                entity.getBirthDate(),
                skills,
                experiences,
                contacts,
                entity.getRole());
    }
}
