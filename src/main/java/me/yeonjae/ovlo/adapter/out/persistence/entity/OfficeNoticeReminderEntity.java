package me.yeonjae.ovlo.adapter.out.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "office_notice_reminder")
public class OfficeNoticeReminderEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "notice_id") public Long noticeId;
    @Column(name = "due_on") public java.time.LocalDate dueOn;
    @Column(name = "overdue") public boolean overdue;
}
