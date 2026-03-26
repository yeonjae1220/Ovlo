package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;
import me.yeonjae.ovlo.domain.member.model.DegreeType;
import me.yeonjae.ovlo.domain.member.model.MemberStatus;
import me.yeonjae.ovlo.domain.member.model.OAuthProvider;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "member")
public class MemberJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    @Column(nullable = false)
    private String name;

    @Column
    private String hometown;

    @Column(nullable = false, unique = true)
    private String email;

    @Column
    private String password;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private OAuthProvider provider = OAuthProvider.LOCAL;

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "home_university_id")
    private Long homeUniversityId;

    @Column(name = "profile_image_media_id")
    private String profileImageMediaId;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private MemberStatus status;

    @Column(name = "major_name")
    private String majorName;

    @Column(name = "degree_type")
    @Enumerated(EnumType.STRING)
    private DegreeType degreeType;

    @Column(name = "grade_level")
    private Integer gradeLevel;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "member_language_skill",
            joinColumns = @JoinColumn(name = "member_id"))
    private List<LanguageSkillEmbeddable> languageSkills = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "member_university_experience",
            joinColumns = @JoinColumn(name = "member_id"))
    private List<UniversityExperienceEmbeddable> universityExperiences = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "member_contact_info",
            joinColumns = @JoinColumn(name = "member_id"))
    private List<ContactInfoEmbeddable> contactInfos = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public MemberJpaEntity() {}

    // ── getters / setters (JPA 전용) ───────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getHometown() { return hometown; }
    public void setHometown(String hometown) { this.hometown = hometown; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public OAuthProvider getProvider() { return provider; }
    public void setProvider(OAuthProvider provider) { this.provider = provider; }
    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }
    public Long getHomeUniversityId() { return homeUniversityId; }
    public void setHomeUniversityId(Long homeUniversityId) { this.homeUniversityId = homeUniversityId; }
    public String getProfileImageMediaId() { return profileImageMediaId; }
    public void setProfileImageMediaId(String profileImageMediaId) { this.profileImageMediaId = profileImageMediaId; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public MemberStatus getStatus() { return status; }
    public void setStatus(MemberStatus status) { this.status = status; }
    public String getMajorName() { return majorName; }
    public void setMajorName(String majorName) { this.majorName = majorName; }
    public DegreeType getDegreeType() { return degreeType; }
    public void setDegreeType(DegreeType degreeType) { this.degreeType = degreeType; }
    public Integer getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(Integer gradeLevel) { this.gradeLevel = gradeLevel; }
    public List<LanguageSkillEmbeddable> getLanguageSkills() { return languageSkills; }
    public void setLanguageSkills(List<LanguageSkillEmbeddable> languageSkills) { this.languageSkills = languageSkills; }
    public List<UniversityExperienceEmbeddable> getUniversityExperiences() { return universityExperiences; }
    public void setUniversityExperiences(List<UniversityExperienceEmbeddable> universityExperiences) { this.universityExperiences = universityExperiences; }
    public List<ContactInfoEmbeddable> getContactInfos() { return contactInfos; }
    public void setContactInfos(List<ContactInfoEmbeddable> contactInfos) { this.contactInfos = contactInfos; }
}
