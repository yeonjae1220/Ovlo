package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.ConsultationActionCommand;
import me.yeonjae.ovlo.application.dto.command.OpenConsultationCommand;
import me.yeonjae.ovlo.application.dto.result.ConsultationResult;
import me.yeonjae.ovlo.application.port.in.office.ManageConsultationUseCase;
import me.yeonjae.ovlo.application.port.in.office.OpenConsultationUseCase;
import me.yeonjae.ovlo.application.port.out.chat.LoadChatPort;
import me.yeonjae.ovlo.application.port.out.chat.SaveChatPort;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.office.LoadConsultationPort;
import me.yeonjae.ovlo.application.port.out.office.LoadInquiryPort;
import me.yeonjae.ovlo.application.port.out.office.LoadOfficePort;
import me.yeonjae.ovlo.application.port.out.office.OfficeNotificationPort;
import me.yeonjae.ovlo.application.port.out.office.SaveConsultationPort;
import me.yeonjae.ovlo.application.port.out.office.SaveInquiryPort;
import me.yeonjae.ovlo.application.service.office.StudentOfficeAccessPolicy;
import me.yeonjae.ovlo.domain.chat.model.ChatRoom;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomId;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomType;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.office.exception.ConsultationException;
import me.yeonjae.ovlo.domain.office.exception.OfficeException;
import me.yeonjae.ovlo.domain.office.model.Consultation;
import me.yeonjae.ovlo.domain.office.model.Inquiry;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import me.yeonjae.ovlo.domain.office.model.Office;
import me.yeonjae.ovlo.domain.office.model.OfficeStaff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 상담 이관(설계 §5.3). 열기·배정·해결·종료·자동 종료.
 * 대화는 chat 도메인에 그대로 저장하고 여기서는 상태와 동의 기록만 다룬다.
 */
@Service
public class ConsultationCommandService implements OpenConsultationUseCase, ManageConsultationUseCase {

    private static final Logger log = LoggerFactory.getLogger(ConsultationCommandService.class);

    private final StudentOfficeAccessPolicy accessPolicy;
    private final LoadOfficePort loadOfficePort;
    private final LoadConsultationPort loadConsultationPort;
    private final SaveConsultationPort saveConsultationPort;
    private final LoadInquiryPort loadInquiryPort;
    private final SaveInquiryPort saveInquiryPort;
    private final LoadChatPort loadChatPort;
    private final SaveChatPort saveChatPort;
    private final LoadMemberPort loadMemberPort;
    private final OfficeNotificationPort notificationPort;
    private final TransactionTemplate tx;
    private final Clock clock;

    public ConsultationCommandService(StudentOfficeAccessPolicy accessPolicy, LoadOfficePort loadOfficePort,
                                      LoadConsultationPort loadConsultationPort, SaveConsultationPort saveConsultationPort,
                                      LoadInquiryPort loadInquiryPort, SaveInquiryPort saveInquiryPort,
                                      LoadChatPort loadChatPort, SaveChatPort saveChatPort, LoadMemberPort loadMemberPort,
                                      OfficeNotificationPort notificationPort, TransactionTemplate tx, Clock clock) {
        this.accessPolicy = accessPolicy;
        this.loadOfficePort = loadOfficePort;
        this.loadConsultationPort = loadConsultationPort;
        this.saveConsultationPort = saveConsultationPort;
        this.loadInquiryPort = loadInquiryPort;
        this.saveInquiryPort = saveInquiryPort;
        this.loadChatPort = loadChatPort;
        this.saveChatPort = saveChatPort;
        this.loadMemberPort = loadMemberPort;
        this.notificationPort = notificationPort;
        this.tx = tx;
        this.clock = clock;
    }

    // ── 열기 ──────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ConsultationResult open(OpenConsultationCommand c) {
        Office office = accessPolicy.assertMember(c.memberId(), c.officeId());
        List<Long> staffIds = office.activeStaff().stream().map(OfficeStaff::getMemberId).toList();
        if (staffIds.isEmpty()) {
            throw new ConsultationException("이 국제처에는 아직 활동 중인 담당자가 없어 상담을 전달할 수 없습니다",
                    ConsultationException.ErrorType.BAD_REQUEST);
        }

        Optional<Inquiry> inquiry = c.inquiryId() == null ? Optional.empty()
                : loadInquiryPort.findById(c.inquiryId()).filter(i -> i.getOfficeId().equals(c.officeId()));
        if (c.inquiryId() != null && inquiry.isEmpty()) {
            throw new ConsultationException("문의를 찾을 수 없습니다: id=" + c.inquiryId(), ConsultationException.ErrorType.NOT_FOUND);
        }
        if (inquiry.isPresent() && !inquiry.get().isOwnedBy(c.memberId())) {
            throw new ConsultationException("본인의 문의만 전달할 수 있습니다", ConsultationException.ErrorType.FORBIDDEN);
        }
        String question = inquiry.map(Inquiry::getQuestion).orElseGet(() -> requireText(c.message()));
        String answer = inquiry.map(Inquiry::getAnswerText).orElse(null);
        NoticeLang lang = inquiry.map(Inquiry::getLang).orElse(NoticeLang.KO);

        // 채팅방: 학생 + 활동 담당자 전원. 첫 메시지는 학생 이름으로 질문(또는 직접 쓴 메시지)
        List<MemberId> participants = new ArrayList<>();
        participants.add(new MemberId(c.memberId()));
        staffIds.forEach(id -> participants.add(new MemberId(id)));
        ChatRoom room = saveChatPort.save(ChatRoom.create(ChatRoomType.GROUP, "[" + office.getName() + "] 상담", participants));
        String firstMessage = c.message() != null && !c.message().isBlank() ? c.message().trim() : question;
        saveChatPort.saveMessage(room.getId().value(), c.memberId(), firstMessage);

        Instant now = clock.instant();
        Consultation consultation = Consultation.open(c.officeId(), c.memberId(), inquiry.map(Inquiry::getId).orElse(null),
                c.consentVersion(), new Consultation.SharedContext(question, answer, lang, true), room.getId().value(), now);
        Consultation saved = saveConsultationPort.save(consultation);

        inquiry.ifPresent(i -> {
            i.markEscalated();
            saveInquiryPort.save(i);
        });

        List<String> emails = loadMemberPort.findAllByIds(staffIds.stream().map(MemberId::new).toList()).stream()
                .map(m -> m.getEmail().value()).toList();
        notificationPort.sendConsultationOpened(emails, office.getName(), question);
        log.info("[Consultation] opened office={} consultation={} room={} inquiry={} consent={}", c.officeId(),
                saved.getId(), room.getId().value(), c.inquiryId(), c.consentVersion());
        return toResult(saved);
    }

    // ── 담당자·학생 전이 ────────────────────────────────────────────────────

    @Override
    @Transactional
    public ConsultationResult assign(ConsultationActionCommand c) {
        Consultation consultation = requireConsultation(c.officeId(), c.consultationId());
        Office office = loadOfficePort.findById(c.officeId())
                .orElseThrow(() -> new OfficeException("국제처를 찾을 수 없습니다", OfficeException.ErrorType.NOT_FOUND));
        if (office.activeMembershipOf(c.actorMemberId()).isEmpty()) {
            throw new ConsultationException("이 국제처의 활동 담당자만 맡을 수 있습니다", ConsultationException.ErrorType.FORBIDDEN);
        }
        consultation.assign(c.actorMemberId(), clock.instant());
        ensureParticipant(consultation.getChatRoomId(), c.actorMemberId());
        Consultation saved = saveConsultationPort.save(consultation);
        log.info("[Consultation] assigned office={} consultation={} staff={}", c.officeId(), c.consultationId(), c.actorMemberId());
        return toResult(saved);
    }

    @Override
    @Transactional
    public ConsultationResult resolve(ConsultationActionCommand c) {
        Consultation consultation = requireConsultation(c.officeId(), c.consultationId());
        consultation.resolve(clock.instant());
        Consultation saved = saveConsultationPort.save(consultation);
        log.info("[Consultation] resolved office={} consultation={} by={}", c.officeId(), c.consultationId(), c.actorMemberId());
        return toResult(saved);
    }

    @Override
    @Transactional
    public ConsultationResult close(ConsultationActionCommand c) {
        Consultation consultation = requireConsultation(c.officeId(), c.consultationId());
        if (!consultation.isOwnedBy(c.actorMemberId())) {
            throw new ConsultationException("본인의 상담만 종료할 수 있습니다", ConsultationException.ErrorType.FORBIDDEN);
        }
        consultation.close(clock.instant());
        Consultation saved = saveConsultationPort.save(consultation);
        log.info("[Consultation] closed by student office={} consultation={}", c.officeId(), c.consultationId());
        return toResult(saved);
    }

    /** 건별 트랜잭션 — 한 건의 실패가 나머지를 되돌리지 않는다(GLOBAL-PIT-128). */
    @Override
    public int closeDue() {
        Instant now = clock.instant();
        Instant cutoff = now.minus(Consultation.AUTO_CLOSE_AFTER_RESOLVED);
        int closed = 0;
        for (Long id : loadConsultationPort.findIdsResolvedBefore(cutoff)) {
            try {
                Boolean did = tx.execute(status -> loadConsultationPort.findById(id).map(cons -> {
                    if (!cons.isAutoCloseDue(now)) return false;
                    cons.close(now);
                    saveConsultationPort.save(cons);
                    return true;
                }).orElse(false));
                if (Boolean.TRUE.equals(did)) closed++;
            } catch (RuntimeException e) {
                log.error("[Consultation] auto-close failed consultation={} (다음 실행에서 재시도)", id, e);
            }
        }
        if (closed > 0) log.info("[Consultation] auto-closed {} consultation(s)", closed);
        return closed;
    }

    // ── 내부 ──────────────────────────────────────────────────────────────────

    private Consultation requireConsultation(Long officeId, Long consultationId) {
        return loadConsultationPort.findById(consultationId)
                .filter(c -> c.getOfficeId().equals(officeId))
                .orElseThrow(() -> new ConsultationException("상담을 찾을 수 없습니다: id=" + consultationId,
                        ConsultationException.ErrorType.NOT_FOUND));
    }

    private void ensureParticipant(Long chatRoomId, Long memberId) {
        ChatRoom room = loadChatPort.findById(new ChatRoomId(chatRoomId)).orElse(null);
        if (room != null && room.addParticipant(new MemberId(memberId))) {
            saveChatPort.save(room);
        }
    }

    private ConsultationResult toResult(Consultation c) {
        String nickname = c.getAssignedStaffMemberId() == null ? null
                : loadMemberPort.findById(new MemberId(c.getAssignedStaffMemberId())).map(m -> m.getNickname()).orElse(null);
        return ConsultationResult.from(c, nickname);
    }

    private static String requireText(String message) {
        if (message == null || message.isBlank()) {
            throw new ConsultationException("전달할 질문이나 메시지가 필요합니다", ConsultationException.ErrorType.BAD_REQUEST);
        }
        return message.trim();
    }
}
