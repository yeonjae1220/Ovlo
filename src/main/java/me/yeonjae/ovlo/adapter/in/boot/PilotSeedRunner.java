package me.yeonjae.ovlo.adapter.in.boot;

import me.yeonjae.ovlo.application.dto.command.CreateOfficeCommand;
import me.yeonjae.ovlo.application.dto.command.InviteOfficeStaffCommand;
import me.yeonjae.ovlo.application.dto.command.NoticeActionCommand;
import me.yeonjae.ovlo.application.dto.command.RegisterNoticeCommand;
import me.yeonjae.ovlo.application.dto.command.UpsertPartnerCommand;
import me.yeonjae.ovlo.application.dto.result.NoticeResult;
import me.yeonjae.ovlo.application.dto.result.NoticeSummaryResult;
import me.yeonjae.ovlo.application.dto.result.OfficeResult;
import me.yeonjae.ovlo.application.port.in.office.AdministerOfficeUseCase;
import me.yeonjae.ovlo.application.port.in.office.GetNoticeQuery;
import me.yeonjae.ovlo.application.port.in.office.ManageNoticeUseCase;
import me.yeonjae.ovlo.application.port.in.office.ManageOfficeStaffUseCase;
import me.yeonjae.ovlo.application.port.in.office.ManagePartnerUseCase;
import me.yeonjae.ovlo.application.port.out.member.LoadMemberPort;
import me.yeonjae.ovlo.application.port.out.office.LoadOfficePort;
import me.yeonjae.ovlo.domain.member.model.Member;
import me.yeonjae.ovlo.domain.office.model.NoticeCategory;
import me.yeonjae.ovlo.domain.office.model.NoticeStatus;
import me.yeonjae.ovlo.domain.office.model.Office;
import me.yeonjae.ovlo.domain.office.model.OfficeRole;
import me.yeonjae.ovlo.domain.office.model.OfficeStaffStatus;
import me.yeonjae.ovlo.domain.office.model.PartnerCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 파일럿 시드(설계 S6). OVLO_PILOT_SEED_ENABLED=true 로 한 번 기동하면
 * 대학 1곳에 Office · 샘플 승인 자료 3건(KO, 승인됨 → 번역 비동기) · 제휴 4건(꺼짐)을 만든다.
 * 멱등·재개 가능: Office·관리자 초대·샘플 자료(제목 단위, DRAFT 면 승인만)를 각각 '없을 때만' 만든다 — 이전 시드가
 * 중간에 죽었으면(2026-10-31 SMTP 535 로 초대에서 실패) 다시 켜면 거기서부터 이어진다. 제휴는 플랫폼 공통이라 하나라도 있으면 건너뛴다.
 * 기존 유스케이스만 호출하므로 화면에서 만든 것과 같은 상태다. 실패는 삼키지 않는다 — 기동이 멈추는 편이 낫다.
 */
@Component
@ConditionalOnProperty(name = "ovlo.office.pilot-seed.enabled", havingValue = "true")
public class PilotSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PilotSeedRunner.class);
    static final ZoneId OFFICE_ZONE = ZoneId.of("Asia/Seoul");
    static final String SAMPLE_PREFIX = "[예시] ";
    private static final int SAMPLE_SCAN_LIMIT = 200;

    private final AdministerOfficeUseCase administerOffice;
    private final ManageOfficeStaffUseCase manageStaff;
    private final ManageNoticeUseCase manageNotice;
    private final GetNoticeQuery noticeQuery;
    private final ManagePartnerUseCase managePartner;
    private final LoadOfficePort loadOfficePort;
    private final LoadMemberPort loadMemberPort;
    private final Clock clock;
    private final long universityId;
    private final String officeName;
    private final String adminEmail;

    public PilotSeedRunner(AdministerOfficeUseCase administerOffice, ManageOfficeStaffUseCase manageStaff,
                           ManageNoticeUseCase manageNotice, GetNoticeQuery noticeQuery, ManagePartnerUseCase managePartner,
                           LoadOfficePort loadOfficePort, LoadMemberPort loadMemberPort, Clock clock,
                           @Value("${ovlo.office.pilot-seed.university-id:0}") long universityId,
                           @Value("${ovlo.office.pilot-seed.office-name:파일럿 대학 국제처}") String officeName,
                           @Value("${ovlo.office.pilot-seed.admin-email:}") String adminEmail) {
        this.administerOffice = administerOffice;
        this.manageStaff = manageStaff;
        this.manageNotice = manageNotice;
        this.noticeQuery = noticeQuery;
        this.managePartner = managePartner;
        this.loadOfficePort = loadOfficePort;
        this.loadMemberPort = loadMemberPort;
        this.clock = clock;
        this.universityId = universityId;
        this.officeName = officeName;
        this.adminEmail = adminEmail == null ? "" : adminEmail.trim();
    }

    @Override
    public void run(ApplicationArguments args) {
        if (universityId <= 0) {
            throw new IllegalStateException("OVLO_PILOT_SEED_UNIVERSITY_ID 가 필요합니다 (global_universities.id)");
        }
        seedPartners();
        Optional<Office> existing = loadOfficePort.findByUniversityId(universityId);
        Long officeId;
        if (existing.isPresent()) {
            officeId = existing.get().getId().value();
            log.info("[seed] 대학 {} 에 Office(id={}) 가 이미 있어 초대·자료만 확인합니다", universityId, officeId);
        } else {
            OfficeResult office = administerOffice.create(new CreateOfficeCommand(universityId, officeName));
            officeId = office.id();
            log.info("[seed] Office 생성 id={} name={}", office.id(), office.name());
        }

        Optional<Member> admin = adminEmail.isEmpty() ? Optional.empty() : loadMemberPort.findByEmail(adminEmail);
        if (admin.isEmpty()) {
            log.warn("[seed] 관리자 이메일이 비었거나 가입된 회원이 아니라 담당자 초대와 샘플 자료를 건너뜁니다: '{}'", adminEmail);
            return;
        }
        Long adminId = admin.get().getId().value();

        // REMOVED 는 담당자가 아니다 — Office.invite 가 재초대로 처리한다
        boolean alreadyStaff = existing.flatMap(o -> o.findStaff(adminId))
                .filter(s -> s.getStatus() != OfficeStaffStatus.REMOVED).isPresent();
        if (alreadyStaff) {
            log.info("[seed] 회원 {} 은 이미 담당자(초대됨/활동)라 초대를 건너뜁니다", adminId);
        } else {
            manageStaff.invite(new InviteOfficeStaffCommand(officeId, adminEmail, OfficeRole.OFFICE_ADMIN, "seed"));
        }

        Map<String, NoticeSummaryResult> samplesPresent = existing.isEmpty() ? Map.of()
                : noticeQuery.findForStaff(officeId, null, null, PageRequest.of(0, SAMPLE_SCAN_LIMIT)).getContent().stream()
                        .filter(n -> n.title().startsWith(SAMPLE_PREFIX))
                        .collect(Collectors.toMap(NoticeSummaryResult::title, n -> n, (a, b) -> a));
        seedNotices(officeId, adminId, samplesPresent);
    }

    private void seedPartners() {
        if (!managePartner.findAll().isEmpty()) {
            log.info("[seed] 제휴가 이미 있어 건너뜁니다");
            return;
        }
        List<UpsertPartnerCommand> partners = List.of(
                new UpsertPartnerCommand(PartnerCategory.SIM, "유심 (예시)", "입국 전 예약, 공항 수령", "https://example.com/sim", 0),
                new UpsertPartnerCommand(PartnerCategory.INSURANCE, "유학생 보험 (예시)", "외국인 유학생 의무 가입 보험", "https://example.com/insurance", 0),
                new UpsertPartnerCommand(PartnerCategory.HOUSING, "숙소 (예시)", "학교 근처 단기 임대", "https://example.com/housing", 0),
                new UpsertPartnerCommand(PartnerCategory.PICKUP, "공항 픽업 (예시)", "인천공항 → 캠퍼스", "https://example.com/pickup", 0));
        partners.forEach(managePartner::create);
        log.info("[seed] 제휴 {}건 등록(꺼짐) — /admin/partners 에서 실제 링크로 바꾸고 켠다", partners.size());
    }

    /** 제목이 같은 샘플이 있으면 건너뛰고, DRAFT 로 남아 있으면 승인만 한다 — 중간에 죽은 시드를 마저 끝내기 위해. */
    private void seedNotices(Long officeId, Long createdBy, Map<String, NoticeSummaryResult> present) {
        LocalDate today = LocalDate.now(clock.withZone(OFFICE_ZONE));
        String term = today.getYear() + "-" + (today.getMonthValue() <= 6 ? "1" : "2");
        LocalDate until = today.plusMonths(4);
        List<RegisterNoticeCommand> notices = List.of(
                new RegisterNoticeCommand(officeId, NoticeCategory.ARRIVAL, "[예시] 입국 후 14일 안에 할 일",
                        "외국인등록은 입국일로부터 90일 안에 관할 출입국·외국인청에 신청합니다. 국제처 안내 데스크에서 예약을 도와드립니다.\n"
                        + "필요 서류: 여권, 표준입학허가서, 사진 1장, 수수료.", null, List.of(), term, today, until, createdBy),
                new RegisterNoticeCommand(officeId, NoticeCategory.HOUSING, "[예시] 기숙사 신청 안내",
                        "이번 학기 기숙사 신청은 학교 포털 '생활관' 메뉴에서 합니다. 마감 뒤에는 대기 신청만 가능합니다.",
                        null, List.of(), term, today, until, createdBy),
                new RegisterNoticeCommand(officeId, NoticeCategory.INSURANCE, "[예시] 유학생 건강보험 가입",
                        "외국인 유학생은 입국 뒤 국민건강보험에 당연가입됩니다. 보험료 감면은 학교에서 일괄 신청합니다.",
                        null, List.of(), term, today, until, createdBy));
        int created = 0, approvedOnly = 0;
        for (RegisterNoticeCommand cmd : notices) {
            NoticeSummaryResult have = present.get(cmd.title());
            if (have == null) {
                NoticeResult n = manageNotice.register(cmd);
                manageNotice.approve(new NoticeActionCommand(officeId, n.id(), "seed:" + createdBy));
                created++;
            } else if (have.status() == NoticeStatus.DRAFT) {
                manageNotice.approve(new NoticeActionCommand(officeId, have.id(), "seed:" + createdBy));
                approvedOnly++;
            }
        }
        if (created + approvedOnly == 0) {
            log.info("[seed] 샘플 자료 {}건이 이미 있어 건너뜁니다", notices.size());
        } else {
            log.info("[seed] 샘플 승인 자료 {}건 생성, {}건 승인만 — 제목에 [예시] 가 붙어 있으니 실제 자료로 바꾼다", created, approvedOnly);
        }
    }
}
