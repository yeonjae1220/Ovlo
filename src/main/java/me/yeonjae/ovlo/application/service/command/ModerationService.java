package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.ReportContentCommand;
import me.yeonjae.ovlo.application.dto.result.BlockedMemberPageResult;
import me.yeonjae.ovlo.application.dto.result.BlockedMemberResult;
import me.yeonjae.ovlo.application.dto.result.ReportSummaryResult;
import me.yeonjae.ovlo.application.port.in.moderation.BlockMemberUseCase;
import me.yeonjae.ovlo.application.port.in.moderation.GetBlockedMembersQuery;
import me.yeonjae.ovlo.application.port.in.moderation.ModerateReportUseCase;
import me.yeonjae.ovlo.application.port.in.moderation.ReportContentUseCase;
import me.yeonjae.ovlo.application.port.out.follow.LoadFollowPort;
import me.yeonjae.ovlo.application.port.out.follow.SaveFollowPort;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.member.SaveMemberPort;
import me.yeonjae.ovlo.application.port.out.moderation.ContentReportPort;
import me.yeonjae.ovlo.application.port.out.moderation.MemberBlockPort;
import me.yeonjae.ovlo.application.port.out.moderation.ReportTargetPort;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.MemberStatus;
import me.yeonjae.ovlo.domain.moderation.exception.ModerationException;
import me.yeonjae.ovlo.domain.moderation.model.ContentReport;
import me.yeonjae.ovlo.domain.moderation.model.ReportResolution;
import me.yeonjae.ovlo.domain.moderation.model.ReportStatus;
import me.yeonjae.ovlo.domain.moderation.model.ReportTargetType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static me.yeonjae.ovlo.domain.moderation.exception.ModerationException.ErrorType.BAD_REQUEST;
import static me.yeonjae.ovlo.domain.moderation.exception.ModerationException.ErrorType.CONFLICT;
import static me.yeonjae.ovlo.domain.moderation.exception.ModerationException.ErrorType.NOT_FOUND;

/**
 * 사용자 신고·차단과 관리자 신고 처리 (App Store 1.2 / Google Play UGC 정책 대응).
 */
@Service
@Transactional
public class ModerationService implements ReportContentUseCase, BlockMemberUseCase, GetBlockedMembersQuery, ModerateReportUseCase {

    static final int BLOCK_PAGE_SIZE = 20;
    static final int ADMIN_REPORT_LIMIT = 100;

    private final ContentReportPort reports;
    private final ReportTargetPort targets;
    private final MemberBlockPort blocks;
    private final LoadMemberPort loadMemberPort;
    private final SaveMemberPort saveMemberPort;
    private final LoadFollowPort loadFollowPort;
    private final SaveFollowPort saveFollowPort;

    public ModerationService(ContentReportPort reports, ReportTargetPort targets, MemberBlockPort blocks,
                             LoadMemberPort loadMemberPort, SaveMemberPort saveMemberPort,
                             LoadFollowPort loadFollowPort, SaveFollowPort saveFollowPort) {
        this.reports = reports;
        this.targets = targets;
        this.blocks = blocks;
        this.loadMemberPort = loadMemberPort;
        this.saveMemberPort = saveMemberPort;
        this.loadFollowPort = loadFollowPort;
        this.saveFollowPort = saveFollowPort;
    }

    @Override
    public void report(ReportContentCommand command) {
        long authorId = targets.findAuthorVisibleTo(command.targetType(), command.targetId(), command.reporterId())
                .orElseThrow(() -> new ModerationException("신고할 대상을 찾을 수 없습니다", NOT_FOUND));
        // 차단 관계인 상대의 게시글·댓글은 신고자 화면에 없다. 회원 신고는 차단과 함께 할 수 있어야 하므로 예외.
        if (command.targetType() != ReportTargetType.MEMBER && command.reporterId() != authorId
                && blocks.blockedEitherWay(command.reporterId(), authorId)) {
            throw new ModerationException("신고할 대상을 찾을 수 없습니다", NOT_FOUND);
        }
        reports.save(new ContentReport(command.targetType(), command.targetId(), authorId,
                command.reporterId(), command.reason(), command.detail()));
    }

    @Override
    public void block(long actorId, long targetId) {
        if (actorId == targetId) {
            throw new ModerationException("자기 자신은 차단할 수 없습니다", BAD_REQUEST);
        }
        boolean blockable = loadMemberPort.findById(new MemberId(targetId))
                .map(member -> member.getStatus() != MemberStatus.WITHDRAWN)
                .orElse(false);
        if (!blockable) {
            throw new ModerationException("회원을 찾을 수 없습니다", NOT_FOUND);
        }
        blocks.block(actorId, targetId);
        unfollow(actorId, targetId);
        unfollow(targetId, actorId);
    }

    private void unfollow(long followerId, long followeeId) {
        loadFollowPort.findByFollowerAndFollowee(new MemberId(followerId), new MemberId(followeeId))
                .ifPresent(saveFollowPort::delete);
    }

    @Override
    public void unblock(long actorId, long targetId) {
        blocks.unblock(actorId, targetId);
    }

    @Override
    @Transactional(readOnly = true)
    public BlockedMemberPageResult blockedMembers(long actorId, Long before) {
        if (before != null && before <= 0) {
            throw new ModerationException("올바르지 않은 페이지 커서입니다", BAD_REQUEST);
        }
        List<BlockedMemberResult> rows = blocks.blockedBy(actorId, before, BLOCK_PAGE_SIZE + 1);
        if (rows.size() <= BLOCK_PAGE_SIZE) {
            return new BlockedMemberPageResult(rows, null);
        }
        List<BlockedMemberResult> page = rows.subList(0, BLOCK_PAGE_SIZE);
        return new BlockedMemberPageResult(List.copyOf(page), page.getLast().memberId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlockedByMe(long actorId, long targetId) {
        return blocks.blocked(actorId, targetId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportSummaryResult> reports(ReportStatus status) {
        return reports.findByStatus(status == null ? ReportStatus.OPEN : status, ADMIN_REPORT_LIMIT);
    }

    @Override
    public void resolve(String adminName, long reportId, ReportResolution resolution) {
        if (resolution == null) {
            throw new ModerationException("처리 방법이 필요합니다", BAD_REQUEST);
        }
        ContentReportPort.ReportTarget target = reports.findTarget(reportId)
                .orElseThrow(() -> new ModerationException("신고를 찾을 수 없습니다", NOT_FOUND));
        if (target.status() != ReportStatus.OPEN) {
            throw new ModerationException("이미 처리된 신고입니다", CONFLICT);
        }
        if (!resolution.appliesTo(target.type())) {
            throw new ModerationException("회원 신고에는 콘텐츠 내리기를 쓸 수 없습니다", BAD_REQUEST);
        }
        Member author = resolution == ReportResolution.SUSPEND_AUTHOR ? suspendableAuthor(target.targetAuthorId()) : null;
        // 조건부 UPDATE 로 먼저 닫아, 두 관리자가 동시에 처리해도 조치는 한 번만 일어난다.
        if (reports.resolveOpenReports(target.type(), target.targetId(), resolution, adminName) == 0) {
            throw new ModerationException("이미 처리된 신고입니다", CONFLICT);
        }
        switch (resolution) {
            case REMOVE_CONTENT -> targets.removeContent(target.type(), target.targetId());
            case SUSPEND_AUTHOR -> suspend(author);
            case DISMISS -> { }
        }
    }

    /** 탈퇴한 회원을 정지하면 나중에 정지 해제로 되살아나므로 막는다. */
    private Member suspendableAuthor(long memberId) {
        Member member = loadMemberPort.findById(new MemberId(memberId))
                .orElseThrow(() -> new ModerationException("회원을 찾을 수 없습니다", NOT_FOUND));
        if (member.getStatus() == MemberStatus.WITHDRAWN) {
            throw new ModerationException("이미 탈퇴한 회원입니다. 콘텐츠 내리기나 기각으로 처리하세요", BAD_REQUEST);
        }
        return member;
    }

    /** 이미 정지된 회원이면(다른 신고·관리자 화면에서 먼저 정지) 그대로 둔다. */
    private void suspend(Member member) {
        if (member.getStatus() == MemberStatus.SUSPENDED) {
            return;
        }
        member.suspend();
        saveMemberPort.save(member);
    }
}
