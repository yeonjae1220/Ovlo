package me.yeonjae.ovlo.domain.office.exception;

/**
 * 알림(메일) 전송 실패. 알림은 부수효과라 어느 쪽이 맞는지는 호출부가 정한다:
 * 업무 쓰기를 지켜야 하는 곳(담당자 초대·상담 열기)은 잡고 기록만 남기고,
 * 다음 실행에서 다시 보내면 되는 곳(만료 경고 배치)은 전파해 트랜잭션을 되돌린다.
 */
public class OfficeNotificationException extends RuntimeException {

    private final String kind;

    public OfficeNotificationException(String kind, Throwable cause) {
        super("알림 전송 실패: " + kind, cause);
        this.kind = kind;
    }

    public String getKind() {
        return kind;
    }
}
