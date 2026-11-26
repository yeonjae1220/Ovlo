package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.AdminWithdrawMemberCommand;
import me.yeonjae.ovlo.application.dto.command.WithdrawMemberCommand;
import me.yeonjae.ovlo.application.port.in.member.AdminWithdrawMemberUseCase;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.domain.member.exception.MemberException;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.MemberRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 관리자 수동 탈퇴 — Apple 재인증을 아예 할 수 없는 회원(Apple ID 분실, Apple 장애·키 미설정으로 폐기가 계속
 * 실패)의 대체 경로. 본인 확인은 관리자가 문의 채널에서 먼저 끝낸다.
 *
 * <p>Apple 토큰은 폐기하지 못하므로, 사용자에게 설정 &gt; Apple ID &gt; 'Apple로 로그인' 에서 Ovlo 를 직접
 * 지우도록 안내한다. 감사 기록은 로그 한 줄({@code [AdminWithdraw]})이다.
 */
@Service
public class AdminMemberWithdrawalService implements AdminWithdrawMemberUseCase {

    private static final Logger log = LoggerFactory.getLogger(AdminMemberWithdrawalService.class);
    private static final int MAX_REASON_LENGTH = 500;

    private final LoadMemberPort loadMemberPort;
    private final MemberCommandService memberCommandService;

    public AdminMemberWithdrawalService(LoadMemberPort loadMemberPort, MemberCommandService memberCommandService) {
        this.loadMemberPort = loadMemberPort;
        this.memberCommandService = memberCommandService;
    }

    @Override
    public void withdrawByAdmin(AdminWithdrawMemberCommand command) {
        String reason = requireReason(command.reason());
        Member member = loadMemberPort.findById(new MemberId(command.memberId()))
                .orElseThrow(() -> new MemberException("회원을 찾을 수 없습니다: " + command.memberId(),
                        MemberException.ErrorType.NOT_FOUND));
        if (member.getRole() == MemberRole.ADMIN) {
            throw new MemberException("관리자 계정은 이 화면에서 탈퇴시킬 수 없습니다",
                    MemberException.ErrorType.FORBIDDEN);
        }
        memberCommandService.withdraw(new WithdrawMemberCommand(command.memberId()));
        log.info("[AdminWithdraw] admin={} member={} provider={} appleRevoked=false reason={}",
                command.adminName(), command.memberId(), member.getProvider(), reason);
    }

    /** 감사 로그에 그대로 남으므로 한 줄로 만들어 로그 위조(줄바꿈 주입)를 막는다. */
    private static String requireReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("탈퇴 처리 사유를 입력하세요");
        }
        String oneLine = reason.strip().replaceAll("[\\r\\n\\t]+", " ");
        if (oneLine.length() > MAX_REASON_LENGTH) {
            throw new IllegalArgumentException("사유는 " + MAX_REASON_LENGTH + "자 이내로 입력하세요");
        }
        return oneLine;
    }
}
