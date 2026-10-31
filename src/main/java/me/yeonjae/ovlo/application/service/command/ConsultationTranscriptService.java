package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.result.TranscriptMessageResult;
import me.yeonjae.ovlo.application.port.in.office.GetConsultationTranscriptQuery;
import me.yeonjae.ovlo.application.port.out.chat.LoadChatPort;
import me.yeonjae.ovlo.application.port.out.office.LoadConsultationPort;
import me.yeonjae.ovlo.application.port.out.office.MessageTranslationPort;
import me.yeonjae.ovlo.application.port.out.office.TranslationPort;
import me.yeonjae.ovlo.domain.chat.model.ChatRoomId;
import me.yeonjae.ovlo.domain.chat.model.Message;
import me.yeonjae.ovlo.domain.office.exception.ConsultationException;
import me.yeonjae.ovlo.domain.office.model.Consultation;
import me.yeonjae.ovlo.domain.office.model.NoticeLang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 상담 대화를 요청 언어로 돌려준다. 상대방이 보낸 메시지 중 번역이 없는 것은 지금 번역해 저장한다.
 * - 담당자 메시지의 원문 언어는 한국어로 본다. 학생 메시지는 상담의 공유 언어.
 * - 요청 언어가 원문 언어와 같으면 번역하지 않는다.
 * - 번역 실패는 translationFailed 로 드러낸다(빈 값 치환 금지).
 */
@Service
@Transactional
public class ConsultationTranscriptService implements GetConsultationTranscriptQuery {

    private static final Logger log = LoggerFactory.getLogger(ConsultationTranscriptService.class);
    private static final int MAX_LIMIT = 200;

    private final LoadConsultationPort loadConsultationPort;
    private final LoadChatPort loadChatPort;
    private final MessageTranslationPort messageTranslationPort;
    private final TranslationPort translationPort;

    public ConsultationTranscriptService(LoadConsultationPort loadConsultationPort, LoadChatPort loadChatPort,
                                         MessageTranslationPort messageTranslationPort, TranslationPort translationPort) {
        this.loadConsultationPort = loadConsultationPort;
        this.loadChatPort = loadChatPort;
        this.messageTranslationPort = messageTranslationPort;
        this.translationPort = translationPort;
    }

    @Override
    public List<TranscriptMessageResult> transcript(Long officeId, Long consultationId, Long requesterMemberId, boolean staff,
                                                    NoticeLang lang, int limit) {
        Consultation c = loadConsultationPort.findById(consultationId)
                .filter(x -> x.getOfficeId().equals(officeId))
                .orElseThrow(() -> new ConsultationException("상담을 찾을 수 없습니다: id=" + consultationId,
                        ConsultationException.ErrorType.NOT_FOUND));
        if (!staff && !c.isOwnedBy(requesterMemberId)) {
            throw new ConsultationException("상담 당사자만 볼 수 있습니다", ConsultationException.ErrorType.FORBIDDEN);
        }

        List<Message> messages = loadChatPort.findMessages(new ChatRoomId(c.getChatRoomId()), 0, Math.min(Math.max(limit, 1), MAX_LIMIT));
        List<Long> ids = messages.stream().map(m -> m.getId().value()).toList();
        Map<Long, String> existing = messageTranslationPort.findTranslations(ids, lang);

        List<TranscriptMessageResult> out = new ArrayList<>();
        for (Message m : messages) {
            boolean fromStudent = m.getSenderId().value().equals(c.getMemberId());
            NoticeLang sourceLang = fromStudent ? c.getSharedContext().lang() : NoticeLang.KO;
            boolean mine = m.getSenderId().value().equals(requesterMemberId);
            boolean needsTranslation = !mine && sourceLang != lang;

            String translated = null;
            boolean failed = false;
            if (needsTranslation) {
                translated = existing.get(m.getId().value());
                if (translated == null) {
                    try {
                        TranslationPort.Result r = translationPort.translate(
                                new TranslationPort.Request(sourceLang, lang, "message", m.getContent()));
                        translated = r.body();
                        messageTranslationPort.save(m.getId().value(), lang, translated, r.modelId());
                    } catch (RuntimeException e) {
                        failed = true;
                        log.warn("[Consultation] message translation failed consultation={} message={} lang={}: {}",
                                consultationId, m.getId().value(), lang.code(), e.toString());
                    }
                }
            }
            out.add(new TranscriptMessageResult(m.getId().value(), m.getSenderId().value(), !fromStudent, m.getContent(),
                    translated, failed, m.getSentAt()));
        }
        return out;
    }
}
