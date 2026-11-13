package me.yeonjae.ovlo.application.port.in.office;
import me.yeonjae.ovlo.domain.office.model.NoticeSource;
import me.yeonjae.ovlo.application.port.out.office.NoticeKnowledgePort;
import java.util.List;
public interface ManageNoticeKnowledgeUseCase {
    record ReviewTask(Long id,String title,String state,java.time.LocalDate nextReviewOn,Long ownerMemberId,boolean canBeBasis) {}
    List<ReviewTask> queue(Long office,Long actor);
    void configure(Long office,Long id,Long actor,long revision,NoticeSource source);
    void review(Long office,Long id,Long actor,long revision,String outcome,String evidence,boolean official,boolean scopeChecked,boolean privacyChecked,boolean generalizable);
    NoticeKnowledgePort.Scope saveScope(Long office,Long actor,NoticeKnowledgePort.Scope request);
    List<NoticeKnowledgePort.Scope> scopes(Long office,Long actor);
    List<NoticeKnowledgePort.Audit> history(Long office,Long notice,Long actor);
}
