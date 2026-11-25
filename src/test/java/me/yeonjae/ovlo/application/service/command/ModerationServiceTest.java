package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.ReportContentCommand;
import me.yeonjae.ovlo.application.dto.result.BlockedMemberResult;
import me.yeonjae.ovlo.application.port.out.follow.LoadFollowPort;
import me.yeonjae.ovlo.application.port.out.follow.SaveFollowPort;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.member.SaveMemberPort;
import me.yeonjae.ovlo.application.port.out.moderation.ContentReportPort;
import me.yeonjae.ovlo.application.port.out.moderation.MemberBlockPort;
import me.yeonjae.ovlo.application.port.out.moderation.ReportTargetPort;
import me.yeonjae.ovlo.domain.follow.model.Follow;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.member.model.MemberStatus;
import me.yeonjae.ovlo.domain.moderation.exception.ModerationException;
import me.yeonjae.ovlo.domain.moderation.model.ContentReport;
import me.yeonjae.ovlo.domain.moderation.model.ReportReason;
import me.yeonjae.ovlo.domain.moderation.model.ReportResolution;
import me.yeonjae.ovlo.domain.moderation.model.ReportStatus;
import me.yeonjae.ovlo.domain.moderation.model.ReportTargetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {

    @Mock ContentReportPort reports;
    @Mock ReportTargetPort targets;
    @Mock MemberBlockPort blocks;
    @Mock LoadMemberPort loadMemberPort;
    @Mock SaveMemberPort saveMemberPort;
    @Mock LoadFollowPort loadFollowPort;
    @Mock SaveFollowPort saveFollowPort;

    @InjectMocks ModerationService service;

    private static Member memberWithStatus(MemberStatus status) {
        Member member = mock(Member.class);
        given(member.getStatus()).willReturn(status);
        return member;
    }

    @Nested
    @DisplayName("report()")
    class Report {

        @Test
        @DisplayName("신고자가 볼 수 있는 대상이면 작성자를 고정해 접수한다")
        void savesWithResolvedAuthor() {
            given(targets.findAuthorVisibleTo(ReportTargetType.COMMENT, 30L, 1L)).willReturn(OptionalLong.of(2L));

            service.report(new ReportContentCommand(1L, ReportTargetType.COMMENT, 30L, ReportReason.HARASSMENT, " 욕설 "));

            ArgumentCaptor<ContentReport> saved = ArgumentCaptor.forClass(ContentReport.class);
            verify(reports).save(saved.capture());
            assertThat(saved.getValue().targetAuthorId()).isEqualTo(2L);
            assertThat(saved.getValue().detail()).isEqualTo("욕설");
        }

        @Test
        @DisplayName("차단 관계인 상대의 게시글은 신고자 화면에 없으므로 NOT_FOUND")
        void blockedCounterpartContentIsNotFound() {
            given(targets.findAuthorVisibleTo(ReportTargetType.POST, 5L, 1L)).willReturn(OptionalLong.of(2L));
            given(blocks.blockedEitherWay(1L, 2L)).willReturn(true);

            assertThatThrownBy(() -> service.report(
                    new ReportContentCommand(1L, ReportTargetType.POST, 5L, ReportReason.SPAM, null)))
                    .isInstanceOf(ModerationException.class)
                    .extracting("errorType").isEqualTo(ModerationException.ErrorType.NOT_FOUND);
        }

        @Test
        @DisplayName("차단한 회원도 회원 자체는 신고할 수 있다")
        void canReportBlockedMember() {
            given(targets.findAuthorVisibleTo(ReportTargetType.MEMBER, 2L, 1L)).willReturn(OptionalLong.of(2L));

            service.report(new ReportContentCommand(1L, ReportTargetType.MEMBER, 2L, ReportReason.HARASSMENT, null));

            verify(reports).save(any());
            verify(blocks, never()).blockedEitherWay(anyLong(), anyLong());
        }

        @Test
        @DisplayName("볼 수 없는 대상은 존재 여부를 드러내지 않고 NOT_FOUND")
        void invisibleTargetIsNotFound() {
            given(targets.findAuthorVisibleTo(ReportTargetType.CHAT_MESSAGE, 9L, 1L)).willReturn(OptionalLong.empty());

            assertThatThrownBy(() -> service.report(
                    new ReportContentCommand(1L, ReportTargetType.CHAT_MESSAGE, 9L, ReportReason.SPAM, null)))
                    .isInstanceOf(ModerationException.class)
                    .extracting("errorType").isEqualTo(ModerationException.ErrorType.NOT_FOUND);
            verify(reports, never()).save(any());
        }

        @Test
        @DisplayName("자기 콘텐츠는 신고할 수 없다")
        void cannotReportOwnContent() {
            given(targets.findAuthorVisibleTo(ReportTargetType.POST, 5L, 1L)).willReturn(OptionalLong.of(1L));

            assertThatThrownBy(() -> service.report(
                    new ReportContentCommand(1L, ReportTargetType.POST, 5L, ReportReason.SPAM, null)))
                    .isInstanceOf(ModerationException.class)
                    .extracting("errorType").isEqualTo(ModerationException.ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("block()")
    class Block {

        @Test
        @DisplayName("차단하면 양방향 팔로우를 끊는다")
        void blockRemovesFollowsBothWays() {
            Member target = memberWithStatus(MemberStatus.ACTIVE);
            given(loadMemberPort.findById(new MemberId(2L))).willReturn(Optional.of(target));
            Follow outgoing = Follow.create(new MemberId(1L), new MemberId(2L));
            given(loadFollowPort.findByFollowerAndFollowee(new MemberId(1L), new MemberId(2L))).willReturn(Optional.of(outgoing));
            given(loadFollowPort.findByFollowerAndFollowee(new MemberId(2L), new MemberId(1L))).willReturn(Optional.empty());

            service.block(1L, 2L);

            verify(blocks).block(1L, 2L);
            verify(saveFollowPort).delete(outgoing);
        }

        @Test
        @DisplayName("자기 자신은 차단할 수 없다")
        void cannotBlockSelf() {
            assertThatThrownBy(() -> service.block(1L, 1L))
                    .isInstanceOf(ModerationException.class)
                    .extracting("errorType").isEqualTo(ModerationException.ErrorType.BAD_REQUEST);
            verify(blocks, never()).block(anyLong(), anyLong());
        }

        @Test
        @DisplayName("없는 회원은 NOT_FOUND")
        void unknownTarget() {
            given(loadMemberPort.findById(new MemberId(2L))).willReturn(Optional.empty());

            assertThatThrownBy(() -> service.block(1L, 2L))
                    .isInstanceOf(ModerationException.class)
                    .extracting("errorType").isEqualTo(ModerationException.ErrorType.NOT_FOUND);
        }

        @Test
        @DisplayName("탈퇴한 회원은 차단 대상이 아니다")
        void withdrawnTarget() {
            Member target = memberWithStatus(MemberStatus.WITHDRAWN);
            given(loadMemberPort.findById(new MemberId(2L))).willReturn(Optional.of(target));

            assertThatThrownBy(() -> service.block(1L, 2L))
                    .isInstanceOf(ModerationException.class)
                    .extracting("errorType").isEqualTo(ModerationException.ErrorType.NOT_FOUND);
        }

        @Test
        @DisplayName("차단 해제는 대상 상태와 무관하게 내 차단 행만 지운다")
        void unblock() {
            service.unblock(1L, 2L);

            verify(blocks).unblock(1L, 2L);
        }
    }

    @Nested
    @DisplayName("blockedMembers()")
    class BlockedMembers {

        @Test
        @DisplayName("한 페이지보다 많으면 마지막 회원 ID 를 다음 커서로 준다")
        void pagesWithCursor() {
            List<BlockedMemberResult> rows = LongStream.rangeClosed(1, ModerationService.BLOCK_PAGE_SIZE + 1)
                    .map(i -> 100 - i)
                    .mapToObj(id -> new BlockedMemberResult(id, "m" + id))
                    .toList();
            given(blocks.blockedBy(1L, null, ModerationService.BLOCK_PAGE_SIZE + 1)).willReturn(rows);

            var page = service.blockedMembers(1L, null);

            assertThat(page.content()).hasSize(ModerationService.BLOCK_PAGE_SIZE);
            assertThat(page.nextBefore()).isEqualTo(page.content().getLast().memberId());
        }

        @Test
        @DisplayName("마지막 페이지면 커서가 없다")
        void lastPage() {
            given(blocks.blockedBy(1L, 50L, ModerationService.BLOCK_PAGE_SIZE + 1))
                    .willReturn(List.of(new BlockedMemberResult(10L, "a")));

            assertThat(service.blockedMembers(1L, 50L).nextBefore()).isNull();
        }

        @Test
        @DisplayName("0 이하 커서는 거부한다")
        void rejectsInvalidCursor() {
            assertThatThrownBy(() -> service.blockedMembers(1L, 0L))
                    .isInstanceOf(ModerationException.class);
        }
    }

    @Nested
    @DisplayName("resolve()")
    class Resolve {

        @Test
        @DisplayName("콘텐츠를 내리고 같은 대상의 대기 신고를 함께 닫는다")
        void removeContent() {
            given(reports.findTarget(7L)).willReturn(Optional.of(
                    new ContentReportPort.ReportTarget(ReportTargetType.POST, 5L, 2L, ReportStatus.OPEN)));
            given(reports.resolveOpenReports(ReportTargetType.POST, 5L, ReportResolution.REMOVE_CONTENT, "admin")).willReturn(3);

            service.resolve("admin", 7L, ReportResolution.REMOVE_CONTENT);

            verify(targets).removeContent(ReportTargetType.POST, 5L);
        }

        @Test
        @DisplayName("작성자 정지는 대상 작성자를 정지한다")
        void suspendAuthor() {
            Member author = memberWithStatus(MemberStatus.ACTIVE);
            given(reports.findTarget(7L)).willReturn(Optional.of(
                    new ContentReportPort.ReportTarget(ReportTargetType.MEMBER, 2L, 2L, ReportStatus.OPEN)));
            given(reports.resolveOpenReports(eq(ReportTargetType.MEMBER), eq(2L), eq(ReportResolution.SUSPEND_AUTHOR), any())).willReturn(1);
            given(loadMemberPort.findById(new MemberId(2L))).willReturn(Optional.of(author));

            service.resolve("admin", 7L, ReportResolution.SUSPEND_AUTHOR);

            verify(author).suspend();
            verify(saveMemberPort).save(author);
            verify(targets, never()).removeContent(any(), anyLong());
        }

        @Test
        @DisplayName("이미 정지된 작성자면 정지를 다시 하지 않고 신고만 닫는다")
        void suspendAlreadySuspendedIsNoOp() {
            Member author = memberWithStatus(MemberStatus.SUSPENDED);
            given(reports.findTarget(7L)).willReturn(Optional.of(
                    new ContentReportPort.ReportTarget(ReportTargetType.POST, 5L, 2L, ReportStatus.OPEN)));
            given(loadMemberPort.findById(new MemberId(2L))).willReturn(Optional.of(author));
            given(reports.resolveOpenReports(any(), anyLong(), any(), any())).willReturn(1);

            service.resolve("admin", 7L, ReportResolution.SUSPEND_AUTHOR);

            verify(author, never()).suspend();
            verify(saveMemberPort, never()).save(any());
        }

        @Test
        @DisplayName("탈퇴한 작성자는 정지할 수 없고 신고도 닫지 않는다")
        void cannotSuspendWithdrawnAuthor() {
            Member author = memberWithStatus(MemberStatus.WITHDRAWN);
            given(reports.findTarget(7L)).willReturn(Optional.of(
                    new ContentReportPort.ReportTarget(ReportTargetType.POST, 5L, 2L, ReportStatus.OPEN)));
            given(loadMemberPort.findById(new MemberId(2L))).willReturn(Optional.of(author));

            assertThatThrownBy(() -> service.resolve("admin", 7L, ReportResolution.SUSPEND_AUTHOR))
                    .isInstanceOf(ModerationException.class)
                    .extracting("errorType").isEqualTo(ModerationException.ErrorType.BAD_REQUEST);
            verify(reports, never()).resolveOpenReports(any(), anyLong(), any(), any());
            verify(author, never()).suspend();
        }

        @Test
        @DisplayName("이미 처리된 신고는 CONFLICT, 아무 조치도 하지 않는다")
        void alreadyResolved() {
            given(reports.findTarget(7L)).willReturn(Optional.of(
                    new ContentReportPort.ReportTarget(ReportTargetType.POST, 5L, 2L, ReportStatus.DISMISSED)));

            assertThatThrownBy(() -> service.resolve("admin", 7L, ReportResolution.REMOVE_CONTENT))
                    .isInstanceOf(ModerationException.class)
                    .extracting("errorType").isEqualTo(ModerationException.ErrorType.CONFLICT);
            verify(targets, never()).removeContent(any(), anyLong());
        }

        @Test
        @DisplayName("동시에 다른 관리자가 먼저 닫았으면 CONFLICT")
        void lostRace() {
            given(reports.findTarget(7L)).willReturn(Optional.of(
                    new ContentReportPort.ReportTarget(ReportTargetType.POST, 5L, 2L, ReportStatus.OPEN)));
            given(reports.resolveOpenReports(any(), anyLong(), any(), any())).willReturn(0);

            assertThatThrownBy(() -> service.resolve("admin", 7L, ReportResolution.REMOVE_CONTENT))
                    .isInstanceOf(ModerationException.class)
                    .extracting("errorType").isEqualTo(ModerationException.ErrorType.CONFLICT);
            verify(targets, never()).removeContent(any(), anyLong());
        }

        @Test
        @DisplayName("회원 신고에는 콘텐츠 내리기를 쓸 수 없다")
        void removeContentOnMemberReport() {
            given(reports.findTarget(7L)).willReturn(Optional.of(
                    new ContentReportPort.ReportTarget(ReportTargetType.MEMBER, 2L, 2L, ReportStatus.OPEN)));

            assertThatThrownBy(() -> service.resolve("admin", 7L, ReportResolution.REMOVE_CONTENT))
                    .isInstanceOf(ModerationException.class)
                    .extracting("errorType").isEqualTo(ModerationException.ErrorType.BAD_REQUEST);
        }
    }
}
