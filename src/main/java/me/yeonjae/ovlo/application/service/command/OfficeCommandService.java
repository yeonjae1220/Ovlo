package me.yeonjae.ovlo.application.service.command;

import me.yeonjae.ovlo.application.dto.command.ChangeOfficeStaffRoleCommand;
import me.yeonjae.ovlo.application.dto.command.CreateOfficeCommand;
import me.yeonjae.ovlo.application.dto.command.InviteOfficeStaffCommand;
import me.yeonjae.ovlo.application.dto.command.RemoveOfficeStaffCommand;
import me.yeonjae.ovlo.application.dto.result.OfficeResult;
import me.yeonjae.ovlo.application.dto.result.OfficeStaffResult;
import me.yeonjae.ovlo.application.port.in.office.AcceptOfficeInvitationUseCase;
import me.yeonjae.ovlo.application.port.in.office.AdministerOfficeUseCase;
import me.yeonjae.ovlo.application.port.in.office.ManageOfficeStaffUseCase;
import me.yeonjae.ovlo.application.port.out.auth.TokenStorePort;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.office.LoadOfficePort;
import me.yeonjae.ovlo.application.port.out.office.OfficeNotificationPort;
import me.yeonjae.ovlo.application.port.out.office.SaveOfficePort;
import me.yeonjae.ovlo.application.port.out.university.LoadUniversityPort;
import me.yeonjae.ovlo.application.service.office.OfficeResultMapper;
import me.yeonjae.ovlo.application.service.office.OfficeStaffRemovedEvent;
import org.springframework.context.ApplicationEventPublisher;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.member.model.MemberId;
import me.yeonjae.ovlo.domain.office.exception.OfficeException;
import me.yeonjae.ovlo.domain.office.model.Office;
import me.yeonjae.ovlo.domain.office.model.OfficeMembership;
import me.yeonjae.ovlo.domain.office.model.OfficeStaff;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Office 생성·담당자 관리 유스케이스. 범위 검사(누가 이 Office 를 만질 수 있나)는 어댑터의
 * {@code OfficeAccessPolicy} 가 먼저 하고, 여기서는 상태 규칙(도메인)과 참조 무결성만 본다.
 *
 * 담당자 제거·역할 변경 뒤에는 그 회원의 세션을 지워 다음 요청부터 새 클레임으로 로그인하게 한다(ADR-0005 6항).
 */
@Service
@Transactional
public class OfficeCommandService implements AdministerOfficeUseCase, ManageOfficeStaffUseCase, AcceptOfficeInvitationUseCase {

    private static final Logger log = LoggerFactory.getLogger(OfficeCommandService.class);

    private final LoadOfficePort loadOfficePort;
    private final SaveOfficePort saveOfficePort;
    private final LoadUniversityPort loadUniversityPort;
    private final LoadMemberPort loadMemberPort;
    private final OfficeNotificationPort notificationPort;
    private final TokenStorePort tokenStorePort;
    private final OfficeResultMapper mapper;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public OfficeCommandService(LoadOfficePort loadOfficePort, SaveOfficePort saveOfficePort,
                                LoadUniversityPort loadUniversityPort, LoadMemberPort loadMemberPort,
                                OfficeNotificationPort notificationPort, TokenStorePort tokenStorePort,
                                OfficeResultMapper mapper, ApplicationEventPublisher eventPublisher, Clock clock) {
        this.loadOfficePort = loadOfficePort;
        this.saveOfficePort = saveOfficePort;
        this.loadUniversityPort = loadUniversityPort;
        this.loadMemberPort = loadMemberPort;
        this.notificationPort = notificationPort;
        this.tokenStorePort = tokenStorePort;
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    // ── AdministerOfficeUseCase ──────────────────────────────────────────────

    @Override
    public OfficeResult create(CreateOfficeCommand command) {
        if (!loadUniversityPort.existsById(new UniversityId(command.globalUnivId()))) {
            throw new OfficeException("대학을 찾을 수 없습니다: id=" + command.globalUnivId(),
                    OfficeException.ErrorType.BAD_REQUEST);
        }
        if (loadOfficePort.findByUniversityId(command.globalUnivId()).isPresent()) {
            throw new OfficeException("이 대학에는 이미 국제처 테넌트가 있습니다: univ=" + command.globalUnivId(),
                    OfficeException.ErrorType.CONFLICT);
        }
        Office office = Office.create(command.globalUnivId(), command.name(), clock.instant());
        Office saved = saveOfficePort.save(office);
        log.info("[Office] created id={} univ={}", saved.getId().value(), saved.getGlobalUnivId());
        return mapper.toResult(saved);
    }

    @Override
    public void suspend(Long officeId) {
        Office office = requireOffice(officeId);
        office.suspend();
        saveOfficePort.save(office);
        // 정지되면 담당자 클레임도 무효가 되어야 하므로 활동 담당자 세션을 모두 지운다.
        office.activeStaff().forEach(s -> tokenStorePort.delete(new MemberId(s.getMemberId())));
        log.info("[Office] suspended id={}", officeId);
    }

    @Override
    public void activate(Long officeId) {
        Office office = requireOffice(officeId);
        office.activate();
        saveOfficePort.save(office);
        log.info("[Office] activated id={}", officeId);
    }

    // ── ManageOfficeStaffUseCase ─────────────────────────────────────────────

    @Override
    public OfficeStaffResult invite(InviteOfficeStaffCommand command) {
        Office office = requireOffice(command.officeId());
        Member member = loadMemberPort.findByEmail(command.email())
                .orElseThrow(() -> new OfficeException("가입된 회원만 담당자로 초대할 수 있습니다: " + command.email(),
                        OfficeException.ErrorType.BAD_REQUEST));
        OfficeStaff invited = office.invite(member.getId().value(), command.role(), command.invitedBy(), clock.instant());
        saveOfficePort.save(office);
        notificationPort.sendStaffInvitation(member.getEmail().value(), office.getName(), command.invitedBy());
        log.info("[Office] staff invited office={} member={} role={} by={}",
                command.officeId(), member.getId().value(), command.role(), command.invitedBy());
        return mapper.toStaffResult(invited, member);
    }

    @Override
    public void changeRole(ChangeOfficeStaffRoleCommand command) {
        Office office = requireOffice(command.officeId());
        office.changeRole(command.memberId(), command.role());
        saveOfficePort.save(office);
        tokenStorePort.delete(new MemberId(command.memberId()));
        log.info("[Office] staff role changed office={} member={} role={}",
                command.officeId(), command.memberId(), command.role());
    }

    @Override
    public void remove(RemoveOfficeStaffCommand command) {
        Office office = requireOffice(command.officeId());
        office.remove(command.memberId(), clock.instant());
        saveOfficePort.save(office);
        tokenStorePort.delete(new MemberId(command.memberId()));
        // 커밋 뒤 열린 상담방에서 뺀다(ConsultationEventListener)
        eventPublisher.publishEvent(new OfficeStaffRemovedEvent(command.officeId(), command.memberId()));
        log.info("[Office] staff removed office={} member={}", command.officeId(), command.memberId());
    }

    // ── AcceptOfficeInvitationUseCase ────────────────────────────────────────

    @Override
    public OfficeMembership accept(Long memberId) {
        Office office = loadOfficePort.findOfficeWithInvitationFor(memberId)
                .orElseThrow(() -> new OfficeException("수락할 초대가 없습니다", OfficeException.ErrorType.NOT_FOUND));
        office.accept(memberId, clock.instant());
        Office saved = saveOfficePort.save(office);
        // 기존 세션의 토큰에는 클레임이 없으므로 지워서 다시 로그인하게 한다.
        tokenStorePort.delete(new MemberId(memberId));
        log.info("[Office] invitation accepted office={} member={}", saved.getId().value(), memberId);
        return saved.activeMembershipOf(memberId).orElseThrow();
    }

    private Office requireOffice(Long officeId) {
        return loadOfficePort.findById(officeId)
                .orElseThrow(() -> new OfficeException("국제처를 찾을 수 없습니다: id=" + officeId,
                        OfficeException.ErrorType.NOT_FOUND));
    }
}
