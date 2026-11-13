package me.yeonjae.ovlo.adapter.out.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.yeonjae.ovlo.adapter.out.persistence.entity.*;
import me.yeonjae.ovlo.application.port.out.office.NoticeKnowledgePort;
import me.yeonjae.ovlo.domain.office.model.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import java.time.*;
import java.util.*;

@Component
@Transactional
public class NoticeKnowledgeAdapter implements NoticeKnowledgePort {
    @PersistenceContext private EntityManager em;
    @Override public void lockOffice(Long id) {
        // 모든 자료 변경과 답변 확정은 같은 Office 행을 잠근다. LLM 호출 동안은 잠그지 않는다.
        em.createNativeQuery("SELECT id FROM office WHERE id = ? FOR UPDATE", Long.class).setParameter(1, id).getSingleResult();
        em.clear(); // 잠금 전 영속성 컨텍스트의 오래된 승인본을 재사용하지 않는다.
    }
    @Override public List<Scope> scopes(Long officeId) {
        return em.createQuery("SELECT s FROM OfficeSourceScopeEntity s WHERE s.officeId = :office ORDER BY s.id", OfficeSourceScopeEntity.class)
                .setParameter("office", officeId).getResultList().stream().map(this::scope).toList();
    }
    private Scope scope(OfficeSourceScopeEntity s) { return new Scope(s.id, s.officeId, s.department, s.url, s.enabled, s.revision); }
    @Override public Scope saveScope(Scope value) {
        OfficeSourceScopeEntity s = value.id() == null ? new OfficeSourceScopeEntity() : em.find(OfficeSourceScopeEntity.class, value.id());
        if (s == null || (s.id != null && (!s.officeId.equals(value.officeId()) || s.revision != value.revision())))
            throw new ObjectOptimisticLockingFailureException("SourceScope", value.id());
        s.officeId = value.officeId(); s.department = value.department(); s.url = value.url(); s.enabled = value.enabled();
        if (s.id == null) em.persist(s);
        em.flush(); return scope(s);
    }
    @Override public void audit(Notice n, long actor, String action, String evidence, Instant now) {
        OfficeNoticeAuditEntity a = new OfficeNoticeAuditEntity();
        a.officeId=n.getOfficeId(); a.noticeId=n.getId(); a.sourceVersion=n.getVersion(); a.actorId=actor;
        a.action=action; a.evidence=evidence; a.createdAt=now;
        Map<String,Object> snapshot = new LinkedHashMap<>();
        snapshot.put("title",n.getTitle()); snapshot.put("body",n.getBody()); snapshot.put("sourceUrl",n.getSourceUrl());
        snapshot.put("source",n.getSource()); snapshot.put("review",n.getReview()); snapshot.put("attachments",n.getAttachmentMediaIds());
        snapshot.put("term",n.getTerm()); snapshot.put("effectiveFrom",n.getEffectiveFrom()); snapshot.put("expiresAt",n.getExpiresAt());
        snapshot.put("status",n.getStatus()); snapshot.put("approvedBy",n.getApprovedBy()); snapshot.put("approvedAt",n.getApprovedAt());
        a.snapshot=NoticeJson.write(snapshot); em.persist(a);
    }
    @Override public List<Audit> history(Long office, Long notice) {
        return em.createQuery("SELECT a FROM OfficeNoticeAuditEntity a WHERE a.officeId=:o AND a.noticeId=:n ORDER BY a.id DESC", OfficeNoticeAuditEntity.class)
                .setParameter("o",office).setParameter("n",notice).setMaxResults(100).getResultList().stream()
                .map(a -> new Audit(a.id,a.officeId,a.noticeId,a.sourceVersion,a.actorId,a.action,a.evidence,a.snapshot,a.createdAt)).toList();
    }
    @Override public void saveCitations(Long inquiryId, List<NoticeCitation> citations) {
        OfficeInquiryEvidenceEntity e=new OfficeInquiryEvidenceEntity(); e.id=inquiryId; e.payload=NoticeJson.write(citations); em.persist(e);
    }
    @Override public List<NoticeCitation> citations(Long inquiryId) {
        OfficeInquiryEvidenceEntity e=em.find(OfficeInquiryEvidenceEntity.class,inquiryId);
        return e == null ? List.of() : List.of(NoticeJson.read(e.payload,NoticeCitation[].class));
    }
    @Override public List<Long> reviewDueIds(LocalDate until) {
        return em.createQuery("SELECT n.id FROM OfficeNoticeJpaEntity n WHERE n.status='APPROVED' AND n.nextReviewOn<=:until ORDER BY n.id",Long.class)
                .setParameter("until",until).getResultList();
    }
    @Override public boolean reminderSent(Long noticeId, LocalDate due, boolean overdue) {
        return em.createQuery("SELECT COUNT(r) FROM OfficeNoticeReminderEntity r WHERE r.noticeId=:n AND r.dueOn=:d AND r.overdue=:o",Long.class)
                .setParameter("n",noticeId).setParameter("d",due).setParameter("o",overdue).getSingleResult()>0;
    }
    @Override public void markReminder(Long noticeId, LocalDate due, boolean overdue) {
        OfficeNoticeReminderEntity r=new OfficeNoticeReminderEntity(); r.noticeId=noticeId;r.dueOn=due;r.overdue=overdue;em.persist(r);
    }
}
