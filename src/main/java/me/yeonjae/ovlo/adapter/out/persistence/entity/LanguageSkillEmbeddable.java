package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import me.yeonjae.ovlo.domain.member.model.CefrLevel;

@Embeddable
public class LanguageSkillEmbeddable {

    @Column(name = "language_code", nullable = false)
    private String languageCode;

    @Column(name = "cefr_level", nullable = false)
    @Enumerated(EnumType.STRING)
    private CefrLevel cefrLevel;

    protected LanguageSkillEmbeddable() {}

    public LanguageSkillEmbeddable(String languageCode, CefrLevel cefrLevel) {
        this.languageCode = languageCode;
        this.cefrLevel = cefrLevel;
    }

    public String getLanguageCode() { return languageCode; }
    public CefrLevel getCefrLevel() { return cefrLevel; }
}
