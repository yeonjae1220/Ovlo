package me.yeonjae.ovlo.application.service.office;

import me.yeonjae.ovlo.application.port.out.office.LoadInquiryPort;
import me.yeonjae.ovlo.domain.office.exception.InquiryException;
import me.yeonjae.ovlo.domain.office.model.Inquiry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/** Server-owned history only: never accept previous answers or ownership from the client. */
public final class InquiryHistory {
    public static final int MAX_TURNS = 20;
    private InquiryHistory() {}

    public static List<Inquiry> load(LoadInquiryPort port, Long officeId, Long memberId, Long lastId) {
        List<Inquiry> turns = new ArrayList<>();
        Long id = lastId;
        while (id != null) {
            if (turns.size() >= MAX_TURNS) throw conflict("대화가 너무 깁니다. 새 대화를 시작해 주세요");
            Inquiry turn = port.findById(id).orElseThrow(InquiryHistory::notFound);
            assertOwner(turn, officeId, memberId);
            turns.add(turn);
            id = turn.getPreviousInquiryId();
        }
        Collections.reverse(turns);
        return List.copyOf(turns);
    }

    public static void assertOwner(Inquiry turn, Long officeId, Long memberId) {
        if (!turn.getOfficeId().equals(officeId) || !turn.isOwnedBy(memberId)) throw notFound();
    }

    public static String transcript(List<Inquiry> turns) {
        return turns.stream().map(i -> "Student: " + i.getQuestion() + "\nAI: "
                + (i.getAnswerText() == null ? "NO_GROUNDS" : i.getAnswerText()))
                .collect(Collectors.joining("\n\n"));
    }

    public static InquiryException conflict(String message) {
        return new InquiryException(message, InquiryException.ErrorType.CONFLICT);
    }
    private static InquiryException notFound() {
        return new InquiryException("대화를 찾을 수 없습니다", InquiryException.ErrorType.NOT_FOUND);
    }
}
