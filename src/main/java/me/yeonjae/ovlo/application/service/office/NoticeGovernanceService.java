package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.application.port.out.office.*;
import me.yeonjae.ovlo.domain.office.model.*;
import me.yeonjae.ovlo.domain.office.exception.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import java.time.*;
import java.util.*;

@Service
@Transactional
public class NoticeGovernanceService implements me.yeonjae.ovlo.application.port.in.office.ManageNoticeKnowledgeUseCase {
    private final LoadNoticePort notices;
    private final SaveNoticePort save;
    private final LoadOfficePort offices;
    private final NoticeKnowledgePort knowledge;
    private final Clock clock;
    public NoticeGovernanceService(LoadNoticePort notices, SaveNoticePort save, LoadOfficePort offices,
                                   NoticeKnowledgePort knowledge, Clock clock) {
        this.notices=notices; this.save=save; this.offices=offices; this.knowledge=knowledge; this.clock=clock;
    }
    public List<ReviewTask> queue(Long office,Long actor) {
        authorize(office,actor,false); var today=OfficeDates.today(clock);
        return all(office).stream().filter(n -> n.getStatus()!=NoticeStatus.ARCHIVED).map(n -> {
            var r=n.getReview(); String state;
            if(n.getSource()==null || r.verifiedOn()==null) state="PENDING";
            else if(r.holdReason()!=null) state="HELD";
            else if(n.getStatus()==NoticeStatus.DRAFT) state="PENDING";
            else if(r.nextReviewOn().isBefore(today)) state="OVERDUE";
            else if(r.nextReviewOn().isBefore(today.plusDays(3))) state="DUE_SOON";
            else state="CURRENT";
            return new ReviewTask(n.getId(),n.getTitle(),state,r.nextReviewOn(),n.getSource()==null?null:n.getSource().ownerMemberId(),n.getSource()!=null && n.getSource().basisNoticeIds().isEmpty() && eligible(n,today));
        }).toList();
    }
    public List<NoticeKnowledgePort.Scope> scopes(Long office,Long actor) { authorize(office,actor,false); return knowledge.scopes(office); }
    public List<NoticeKnowledgePort.Audit> history(Long office,Long notice,Long actor) { authorize(office,actor,false);require(office,notice);return knowledge.history(office,notice); }
    public Office authorize(Long officeId, Long actor, boolean admin) {
        Office office=offices.findById(officeId).orElseThrow(() -> new OfficeException("국제처 없음",OfficeException.ErrorType.NOT_FOUND));
        OfficeMembership membership=office.activeMembershipOf(actor).orElseThrow(() -> new OfficeException("현재 담당자 권한이 없습니다",OfficeException.ErrorType.FORBIDDEN));
        if (admin && !membership.isAdmin()) throw new OfficeException("국제처 관리자만 할 수 있습니다",OfficeException.ErrorType.FORBIDDEN);
        return office;
    }
    public Notice require(Long office, Long id) {
        return notices.findById(id).filter(n -> n.getOfficeId().equals(office)).orElseThrow(() -> new NoticeException("자료 없음",NoticeException.ErrorType.NOT_FOUND));
    }
    public void configure(Long office, Long id, Long actor, long revision, NoticeSource source) {
        knowledge.lockOffice(office); Office o=authorize(office,actor,false); Notice n=require(office,id); n.expectRevision(revision);
        requireOwner(o,source.ownerMemberId()); validateSource(n,source);
        n.configureSource(source,clock.instant());
        if (!source.publicSource() && n.getSourceUrl()!=null) throw new IllegalArgumentException("비공개 원문 URL을 공개 출처에 넣을 수 없습니다");
        save.save(n); knowledge.audit(n,actor,"SOURCE_CHANGED","출처 정보 수정",clock.instant()); invalidateDependents(office,id);
    }
    public void review(Long office, Long id, Long actor, long revision, String outcome, String evidence,
                       boolean official, boolean scopeChecked, boolean privacyChecked, boolean generalizable) {
        knowledge.lockOffice(office); Office o=authorize(office,actor,false); Notice n=require(office,id); n.expectRevision(revision);
        evidence=NoticeSource.text(evidence,"확인 근거/사유",2000);
        if ("VERIFIED".equals(outcome)) {
            if (!official || !scopeChecked) throw new IllegalArgumentException("담당 부서와 적용 범위를 확인해야 합니다");
            if (n.getSource()==null) throw new IllegalArgumentException("출처 정보를 먼저 등록하세요");
            if (n.getSource().type()==NoticeSource.Type.REVIEWED_STAFF_QA && (!privacyChecked || !generalizable))
                throw new IllegalArgumentException("Q&A의 개인정보 제거·재사용 권한·일반화 여부를 확인하세요");
            if (n.getReview().holdReason()!=null && !n.getReview().holdReason().equals("출처 검토 필요") && n.getStatus()==NoticeStatus.APPROVED)
                throw new IllegalArgumentException("중지된 자료는 재승인해야 합니다");
            requireOwner(o,n.getSource().ownerMemberId()); validateSource(n,n.getSource());
            n.verifySource(OfficeDates.today(clock));
        } else if ("FAILED".equals(outcome)) {
            // 검토 실패는 확인일과 기한을 변경하지 않는다.
        } else if (Set.of("CHANGED","WITHDRAWN","CONFLICT").contains(outcome)) {
            n.holdSource(outcome + ": " + evidence);
            // 재검토 후 재승인 경로를 열되 과거 승인본은 감사 이력에 남긴다.
            n.requireReapproval(clock.instant());
        } else throw new IllegalArgumentException("지원하지 않는 검토 결과입니다");
        save.save(n); knowledge.audit(n,actor,outcome,evidence,clock.instant());
        if (!"VERIFIED".equals(outcome) && !"FAILED".equals(outcome)) invalidateDependents(office,id);
    }
    public void validateApproval(Notice n) {
        if (n.getSource()==null || !n.sourceEligible(OfficeDates.today(clock))) throw new IllegalArgumentException("현재 버전의 출처 검토가 필요합니다");
        validateSource(n,n.getSource());
    }
    private void requireOwner(Office o, Long owner) {
        if (o.activeMembershipOf(owner).isEmpty()) throw new IllegalArgumentException("갱신 담당자는 현재 국제처 담당자여야 합니다");
    }
    private void validateSource(Notice n, NoticeSource source) {
        if (source.publicSource() && n.getSourceUrl()!=null) NoticeSource.publicUri(n.getSourceUrl());
        if (!source.publicSource() && n.getSourceUrl()!=null) throw new IllegalArgumentException("비공개 출처 링크는 공개할 수 없습니다");
        if (source.type()==NoticeSource.Type.UNIVERSITY_WEB) {
            boolean allowed=knowledge.scopes(n.getOfficeId()).stream().anyMatch(s -> s.id().equals(source.scopeId()) && s.enabled()
                    && s.department().equals(source.department()) && NoticeSource.withinScope(n.getSourceUrl(),s.url()));
            if (!allowed) throw new IllegalArgumentException("관리자가 허용한 공식 페이지 범위를 확인하세요");
        }
        for (Long basis:source.basisNoticeIds()) {
            if (basis.equals(n.getId())) throw new IllegalArgumentException("자기 자신을 기반 자료로 지정할 수 없습니다");
            Notice parent=require(n.getOfficeId(),basis);
            // 순환 및 장거리 연쇄를 막는다. 파일럿 Q&A는 직접 승인 자료만 참조한다.
            if (parent.getSource()==null || !parent.getSource().basisNoticeIds().isEmpty() || !eligible(parent,OfficeDates.today(clock)))
                throw new IllegalArgumentException("기반 자료는 현재 유효한 직접 승인 자료여야 합니다");
        }
    }
    @Transactional(readOnly=true)
    public boolean eligible(Notice n, LocalDate today) {
        return eligible(n,today,new HashSet<>());
    }
    private boolean eligible(Notice n, LocalDate today, Set<Long> visited) {
        if (!n.isActiveOn(today) || (n.getId()!=null && !visited.add(n.getId()))) return false;
        NoticeSource source=n.getSource();
        if (source.type()==NoticeSource.Type.UNIVERSITY_WEB && knowledge.scopes(n.getOfficeId()).stream().noneMatch(s ->
                s.enabled() && s.id().equals(source.scopeId()) && s.department().equals(source.department()) && NoticeSource.withinScope(n.getSourceUrl(),s.url()))) return false;
        for (Long id:source.basisNoticeIds()) {
            Notice parent=notices.findById(id).filter(x -> x.getOfficeId().equals(n.getOfficeId())).orElse(null);
            if (parent==null || !eligible(parent,today,new HashSet<>(visited))) return false;
        }
        return true;
    }
    public List<Notice> all(Long office) {
        List<Notice> result=new ArrayList<>();
        for(int page=0;;page++) { var rows=notices.findByOffice(office,null,null,PageRequest.of(page,100));
            result.addAll(rows.getContent()); if (!rows.hasNext()) return result; }
    }
    public void invalidateDependents(Long office, Long changedId) {
        for (Notice dependent:all(office)) if (dependent.getSource()!=null && dependent.getSource().basisNoticeIds().contains(changedId)
                && dependent.getStatus()!=NoticeStatus.ARCHIVED) {
            dependent.holdSource("기반 자료가 변경되었습니다"); dependent.requireReapproval(clock.instant()); save.save(dependent);
            knowledge.audit(dependent,0,"BASIS_CHANGED","기반 자료 " + changedId,clock.instant());
        }
    }
    public NoticeKnowledgePort.Scope saveScope(Long office, Long actor, NoticeKnowledgePort.Scope request) {
        knowledge.lockOffice(office); authorize(office,actor,true);
        String url=NoticeSource.publicUri(request.url()).toString();
        if (java.net.URI.create(url).getQuery()!=null) throw new IllegalArgumentException("페이지 범위에는 쿼리를 넣을 수 없습니다");
        var saved=knowledge.saveScope(new NoticeKnowledgePort.Scope(request.id(),office,
                NoticeSource.text(request.department(),"발행 부서",200),url,request.enabled(),request.revision()));
        for(Notice n:all(office)) if(n.getSource()!=null && saved.id().equals(n.getSource().scopeId()) && n.getStatus()!=NoticeStatus.ARCHIVED) {
            n.holdSource("공식 페이지 범위 변경: 재검토 필요"); n.requireReapproval(clock.instant()); save.save(n);
            knowledge.audit(n,actor,"SCOPE_CHANGED","페이지 범위 변경",clock.instant()); invalidateDependents(office,n.getId());
        }
        return saved;
    }
}
