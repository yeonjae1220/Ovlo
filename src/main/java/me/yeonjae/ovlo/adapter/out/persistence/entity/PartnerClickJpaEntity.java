package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "partner_click", indexes = @Index(name = "idx_partner_click_office_time", columnList = "office_id, clicked_at"))
public class PartnerClickJpaEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_id", nullable = false) private Long partnerId;
    @Column(name = "office_id", nullable = false) private Long officeId;
    @Column(name = "member_id") private Long memberId;
    @Column(name = "clicked_at", nullable = false) private Instant clickedAt;

    public PartnerClickJpaEntity() {}

    public Long getId() { return id; }
    public Long getPartnerId() { return partnerId; }
    public void setPartnerId(Long partnerId) { this.partnerId = partnerId; }
    public Long getOfficeId() { return officeId; }
    public void setOfficeId(Long officeId) { this.officeId = officeId; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public Instant getClickedAt() { return clickedAt; }
    public void setClickedAt(Instant clickedAt) { this.clickedAt = clickedAt; }
}
