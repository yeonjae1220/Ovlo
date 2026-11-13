package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "office_notice_audit")
public class OfficeNoticeAuditEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "office_id") public Long officeId;
    @Column(name = "notice_id") public Long noticeId;
    @Column(name = "source_version") public int sourceVersion;
    @Column(name = "actor_id") public long actorId;
    @Column(name = "action") public String action;
    @Column(name = "evidence", columnDefinition = "TEXT") public String evidence;
    @Column(name = "snapshot", columnDefinition = "TEXT") public String snapshot;
    @Column(name = "created_at") public java.time.Instant createdAt;
}
