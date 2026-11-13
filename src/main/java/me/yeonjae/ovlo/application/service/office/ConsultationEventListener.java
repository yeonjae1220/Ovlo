package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.application.port.out.office.RevokeOfficeChatAccessPort;
import me.yeonjae.ovlo.application.port.out.office.LoadConsultationPort;
import me.yeonjae.ovlo.application.port.out.office.SaveConsultationPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 다른 도메인의 사건을 상담 상태에 반영한다(커밋 뒤, 새 트랜잭션).
 * - 채팅 메시지: 상담방이고 발신자가 학생이 아니면 담당자 첫 답변 시각을 기록한다
 * - 담당자 제거: 그 Office 의 모든 상담방에서 권한을 회수한다 (본인의 학생 상담은 보존)
 */
@Component
public class ConsultationEventListener {

    private final LoadConsultationPort loadConsultationPort;
    private final SaveConsultationPort saveConsultationPort;
    private final RevokeOfficeChatAccessPort chatAccess;

    public ConsultationEventListener(LoadConsultationPort loadConsultationPort, SaveConsultationPort saveConsultationPort,
                                     RevokeOfficeChatAccessPort chatAccess) {
        this.loadConsultationPort = loadConsultationPort;
        this.saveConsultationPort = saveConsultationPort;
        this.chatAccess = chatAccess;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMessageSent(ChatMessageSentEvent event) {
        loadConsultationPort.findByChatRoomId(event.chatRoomId()).ifPresent(c -> {
            if (!c.isOwnedBy(event.senderId()) && c.getFirstStaffReplyAt() == null && c.isActive()) {
                c.recordStaffReply(event.sentAt());
                saveConsultationPort.save(c);
            }
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onStaffRemoved(OfficeStaffRemovedEvent event) {
        chatAccess.revoke(event.officeId(), event.memberId());
    }
}
