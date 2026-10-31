package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.application.port.out.chat.LoadChatPort;
import me.yeonjae.ovlo.application.port.out.chat.SaveChatPort;
import me.yeonjae.ovlo.application.port.out.office.LoadConsultationPort;
import me.yeonjae.ovlo.application.port.out.office.SaveConsultationPort;
import me.yeonjae.ovlo.domain.chat.model.ChatRoom;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomId;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.office.model.Consultation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 다른 도메인의 사건을 상담 상태에 반영한다(커밋 뒤, 새 트랜잭션).
 * - 채팅 메시지: 상담방이고 발신자가 학생이 아니면 담당자 첫 답변 시각을 기록한다
 * - 담당자 제거: 그 Office 의 열린 상담방에서 뺀다 (참여자 2명 미만이 되는 방은 남긴다)
 */
@Component
public class ConsultationEventListener {

    private static final Logger log = LoggerFactory.getLogger(ConsultationEventListener.class);

    private final LoadConsultationPort loadConsultationPort;
    private final SaveConsultationPort saveConsultationPort;
    private final LoadChatPort loadChatPort;
    private final SaveChatPort saveChatPort;

    public ConsultationEventListener(LoadConsultationPort loadConsultationPort, SaveConsultationPort saveConsultationPort,
                                     LoadChatPort loadChatPort, SaveChatPort saveChatPort) {
        this.loadConsultationPort = loadConsultationPort;
        this.saveConsultationPort = saveConsultationPort;
        this.loadChatPort = loadChatPort;
        this.saveChatPort = saveChatPort;
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
        MemberId removed = new MemberId(event.memberId());
        for (Consultation c : loadConsultationPort.findActiveByOffice(event.officeId())) {
            ChatRoom room = loadChatPort.findById(new ChatRoomId(c.getChatRoomId())).orElse(null);
            if (room == null || !room.hasParticipant(removed)) continue;
            try {
                if (room.removeParticipant(removed)) saveChatPort.save(room);
            } catch (IllegalStateException e) {
                log.warn("[Consultation] staff {} kept in room {} (would leave < 2 participants)", event.memberId(), c.getChatRoomId());
            }
        }
    }
}
