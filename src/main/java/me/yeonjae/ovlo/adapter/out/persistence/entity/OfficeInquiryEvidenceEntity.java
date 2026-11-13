package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "office_inquiry_evidence")
public class OfficeInquiryEvidenceEntity {
    @Id public Long id;
    @Column(name = "payload", columnDefinition = "TEXT") public String payload;
}
