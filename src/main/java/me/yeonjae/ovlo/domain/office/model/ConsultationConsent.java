package me.yeonjae.ovlo.domain.office.model;

import me.yeonjae.ovlo.domain.office.exception.ConsultationException;

import java.util.Map;

/**
 * 상담 이관 동의 문구의 정본(한국어). 프론트(frontend/src/views/legal/legalContent.ts CONSULTATION_CONSENT)가
 * 학생 언어로 보여 주는 문구와 같은 version 을 쓰고, 서버는 version 과 이 정본을 함께 기록한다.
 * 문구를 바꾸면 양쪽에 새 version 을 추가한다 — 옛 동의 기록은 옛 version 으로 남는다.
 */
public final class ConsultationConsent {

    public static final String CURRENT_VERSION = "v3";

    private static final Map<String, String> TEXTS = Map.of(
            "v1", """
                    아래 내용이 소속 대학 국제처 담당자에게 전달됩니다.
                    - 지금 질문한 내용과 자동 안내 답변
                    - 사용 언어
                    - 학교 이메일 인증 여부(이메일 주소 자체는 전달하지 않음)
                    - 이후 상담방에서 주고받는 메시지
                    담당자의 답변은 내 언어로 번역되어 표시되며 원문도 볼 수 있습니다.
                    상담 내용은 학기 종료 후 1년까지 보관한 뒤 본문을 삭제합니다. 상담 종료 전에는 언제든 종료를 요청할 수 있습니다.
                    동의하지 않아도 자동 안내는 계속 이용할 수 있습니다.""",
            "v2", """
                    아래 내용이 소속 대학 국제처 담당자에게 전달됩니다.
                    - 선택한 대화의 모든 질문과 자동 안내 답변(최대 20회)
                    - 사용 언어
                    - 학교 이메일 인증 여부(이메일 주소 자체는 전달하지 않음)
                    - 이후 상담방에서 주고받는 메시지
                    담당자의 답변은 내 언어로 번역되어 표시되며 원문도 볼 수 있습니다.
                    상담 내용은 학기 종료 후 1년까지 보관한 뒤 본문을 삭제합니다. 상담 종료 전에는 언제든 종료를 요청할 수 있습니다.
                    동의하지 않아도 자동 안내는 계속 이용할 수 있습니다.""",
            "v3", """
                    아래 내용이 소속 대학 국제처 담당자에게 전달됩니다.
                    - 선택한 대화의 모든 질문과 자동 안내 답변(최대 20회)
                    - 사용 언어
                    - 학교 이메일 인증 여부(이메일 주소 자체는 전달하지 않음)
                    - 상담방에 표시되는 내 닉네임과 프로필 사진
                    - 이후 상담방에서 주고받는 메시지
                    담당자에게는 새 상담이 왔다는 알림 메일에 첫 질문이 함께 전달됩니다.
                    주고받는 메시지는 외부 AI 서비스로 번역되며, 이름·이메일 같은 식별 정보는 번역에 보내지 않습니다. 원문도 볼 수 있습니다.
                    상담 내용은 상담을 연 날부터 1년이 지나면 본문을 삭제합니다. 탈퇴하면 바로 삭제합니다. 상담 종료 전에는 언제든 종료를 요청할 수 있습니다.
                    동의하지 않아도 자동 안내는 계속 이용할 수 있습니다.""");

    /** 대화 전체(최대 20회)를 담당자에게 넘기는 동의 버전. v1 은 마지막 질문과 답변만 넘긴다. */
    private static final java.util.Set<String> FULL_CONVERSATION = java.util.Set.of("v2", "v3");

    private ConsultationConsent() {}

    public static boolean sharesFullConversation(String version) {
        return version != null && FULL_CONVERSATION.contains(version);
    }

    public static String textFor(String version) {
        String text = version == null ? null : TEXTS.get(version);
        if (text == null) {
            throw new ConsultationException("알 수 없는 동의 문구 버전입니다: " + version,
                    ConsultationException.ErrorType.CONSENT_REQUIRED);
        }
        return text;
    }
}
