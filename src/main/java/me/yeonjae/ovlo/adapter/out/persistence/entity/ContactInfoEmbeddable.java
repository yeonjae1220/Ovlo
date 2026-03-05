package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import me.yeonjae.ovlo.domain.member.model.ContactType;

@Embeddable
public class ContactInfoEmbeddable {

    @Column(name = "contact_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ContactType contactType;

    @Column(name = "contact_value", nullable = false)
    private String value;

    protected ContactInfoEmbeddable() {}

    public ContactInfoEmbeddable(ContactType contactType, String value) {
        this.contactType = contactType;
        this.value = value;
    }

    public ContactType getContactType() { return contactType; }
    public String getValue() { return value; }
}
