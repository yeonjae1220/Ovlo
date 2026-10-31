package me.yeonjae.ovlo.adapter.out.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import me.yeonjae.ovlo.adapter.out.persistence.repository.PartnerClickJpaRepository;
import me.yeonjae.ovlo.application.port.out.office.LoadNoticePort;
import me.yeonjae.ovlo.application.port.out.office.LoadOfficeStatsPort;
import me.yeonjae.ovlo.domain.office.model.InquiryOutcome;
import me.yeonjae.ovlo.domain.office.model.Notice;
import me.yeonjae.ovlo.domain.office.model.NoticeStatus;
import me.yeonjae.ovlo.domain.office.model.PartnerCategory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 대시보드 집계. JPQL 집계 쿼리만 — H2(테스트)와 PostgreSQL(운영) 양쪽에서 같은 결과. */
@Component
@Transactional(readOnly = true)
public class OfficeStatsPersistenceAdapter implements LoadOfficeStatsPort {

    @PersistenceContext
    private EntityManager em;

    private final PartnerClickJpaRepository clickRepository;
    private final LoadNoticePort loadNoticePort;

    public OfficeStatsPersistenceAdapter(PartnerClickJpaRepository clickRepository, LoadNoticePort loadNoticePort) {
        this.clickRepository = clickRepository;
        this.loadNoticePort = loadNoticePort;
    }

    @Override
    public InquiryCounts countInquiries(Long officeId, Instant from, Instant to) {
        List<Object[]> rows = em.createQuery("""
                SELECT i.outcome, COUNT(i) FROM OfficeInquiryJpaEntity i
                 WHERE i.officeId = :o AND i.askedAt >= :f AND i.askedAt < :t GROUP BY i.outcome
                """, Object[].class).setParameter("o", officeId).setParameter("f", from).setParameter("t", to).getResultList();
        long answered = 0, noGrounds = 0, escalated = 0;
        for (Object[] r : rows) {
            long n = (Long) r[1];
            switch (InquiryOutcome.valueOf((String) r[0])) {
                case ANSWERED -> answered = n;
                case NO_GROUNDS -> noGrounds = n;
                case ESCALATED -> escalated = n;
            }
        }
        return new InquiryCounts(answered + noGrounds + escalated, answered, noGrounds, escalated);
    }

    @Override
    public FeedbackCounts countFeedback(Long officeId, Instant from, Instant to) {
        List<Object[]> rows = em.createQuery("""
                SELECT f.helpful, COUNT(f) FROM OfficeInquiryFeedbackJpaEntity f, OfficeInquiryJpaEntity i
                 WHERE f.inquiryId = i.id AND i.officeId = :o AND i.askedAt >= :f AND i.askedAt < :t GROUP BY f.helpful
                """, Object[].class).setParameter("o", officeId).setParameter("f", from).setParameter("t", to).getResultList();
        long helpful = 0, unhelpful = 0;
        for (Object[] r : rows) {
            if (Boolean.TRUE.equals(r[0])) helpful = (Long) r[1]; else unhelpful = (Long) r[1];
        }
        return new FeedbackCounts(helpful, unhelpful);
    }

    @Override
    public List<String> findQuestions(Long officeId, InquiryOutcome outcome, Instant from, Instant to, int limit) {
        return em.createQuery("""
                SELECT i.question FROM OfficeInquiryJpaEntity i
                 WHERE i.officeId = :o AND i.askedAt >= :f AND i.askedAt < :t
                   AND (:outcome IS NULL OR i.outcome = :outcome)
                 ORDER BY i.askedAt DESC
                """, String.class)
                .setParameter("o", officeId).setParameter("f", from).setParameter("t", to)
                .setParameter("outcome", outcome != null ? outcome.name() : null)
                .setMaxResults(limit).getResultList();
    }

    @Override
    public ConsultationCounts countConsultations(Long officeId, Instant from, Instant to) {
        List<Object[]> rows = em.createQuery("""
                SELECT c.status, COUNT(c) FROM OfficeConsultationJpaEntity c
                 WHERE c.officeId = :o AND c.openedAt >= :f AND c.openedAt < :t GROUP BY c.status
                """, Object[].class).setParameter("o", officeId).setParameter("f", from).setParameter("t", to).getResultList();
        long open = 0, inProgress = 0, resolved = 0, closed = 0;
        for (Object[] r : rows) {
            long n = (Long) r[1];
            switch ((String) r[0]) {
                case "OPEN" -> open = n;
                case "IN_PROGRESS" -> inProgress = n;
                case "RESOLVED" -> resolved = n;
                case "CLOSED" -> closed = n;
                default -> { }
            }
        }
        return new ConsultationCounts(open, inProgress, resolved, closed);
    }

    @Override
    public Double averageFirstReplyMinutes(Long officeId, Instant from, Instant to) {
        // 시각 차이를 DB 함수 없이 계산하려고 쌍을 읽어 평균낸다(파일럿 규모: 수백 건)
        List<Object[]> rows = em.createQuery("""
                SELECT c.openedAt, c.firstStaffReplyAt FROM OfficeConsultationJpaEntity c
                 WHERE c.officeId = :o AND c.openedAt >= :f AND c.openedAt < :t AND c.firstStaffReplyAt IS NOT NULL
                """, Object[].class).setParameter("o", officeId).setParameter("f", from).setParameter("t", to).getResultList();
        return averageMinutes(rows);
    }

    @Override
    public Double averageResolveHours(Long officeId, Instant from, Instant to) {
        List<Object[]> rows = em.createQuery("""
                SELECT c.openedAt, c.resolvedAt FROM OfficeConsultationJpaEntity c
                 WHERE c.officeId = :o AND c.openedAt >= :f AND c.openedAt < :t AND c.resolvedAt IS NOT NULL
                """, Object[].class).setParameter("o", officeId).setParameter("f", from).setParameter("t", to).getResultList();
        Double minutes = averageMinutes(rows);
        return minutes == null ? null : minutes / 60.0;
    }

    private static Double averageMinutes(List<Object[]> rows) {
        if (rows.isEmpty()) return null;
        double total = 0;
        for (Object[] r : rows) {
            total += java.time.Duration.between((Instant) r[0], (Instant) r[1]).toSeconds() / 60.0;
        }
        return total / rows.size();
    }

    @Override
    public Map<PartnerCategory, Long> countPartnerClicks(Long officeId, Instant from, Instant to) {
        Map<PartnerCategory, Long> out = new EnumMap<>(PartnerCategory.class);
        for (Object[] r : clickRepository.countByCategory(officeId, from, to)) {
            out.put(PartnerCategory.valueOf((String) r[0]), (Long) r[1]);
        }
        return out;
    }

    @Override
    public List<Notice> findNoticesExpiringWithin(Long officeId, LocalDate today, int days) {
        LocalDate until = today.plusDays(days);
        return loadNoticePort.findByOffice(officeId, NoticeStatus.APPROVED, null, PageRequest.of(0, 500)).getContent().stream()
                .filter(n -> !n.getExpiresAt().isBefore(today) && !n.getExpiresAt().isAfter(until))
                .sorted(java.util.Comparator.comparing(Notice::getExpiresAt))
                .toList();
    }

    @Override
    public long countNotices(Long officeId, NoticeStatus status) {
        return em.createQuery("SELECT COUNT(n) FROM OfficeNoticeJpaEntity n WHERE n.officeId = :o AND n.status = :s", Long.class)
                .setParameter("o", officeId).setParameter("s", status.name()).getSingleResult();
    }
}
