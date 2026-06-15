package me.yeonjae.ovlo.adapter.in.web;

import lombok.RequiredArgsConstructor;
import me.yeonjae.ovlo.adapter.in.web.dto.response.AdminMemberResponse;
import me.yeonjae.ovlo.application.dto.command.IssueManualVerificationCommand;
import me.yeonjae.ovlo.application.service.admin.AdminService;
import me.yeonjae.ovlo.application.service.admin.AdminVerificationService;
import me.yeonjae.ovlo.domain.member.exception.MemberException;
import me.yeonjae.ovlo.domain.member.model.MemberRole;
import me.yeonjae.ovlo.domain.verification.exception.VerificationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminPageController {

    private static final Logger log = LoggerFactory.getLogger(AdminPageController.class);

    private final AdminService adminService;
    private final AdminVerificationService adminVerificationService;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        var stats = adminService.getStats();
        model.addAttribute("stats", stats);
        return "admin/dashboard";
    }

    @GetMapping("/members")
    public String members(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<AdminMemberResponse> memberPage = adminService.getMembers(
                PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "id")));
        model.addAttribute("members", memberPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", memberPage.getTotalPages());
        model.addAttribute("totalElements", memberPage.getTotalElements());
        return "admin/members";
    }

    @PostMapping("/members/{id}/role")
    public String updateRole(@PathVariable Long id,
                             @RequestParam MemberRole role,
                             RedirectAttributes redirectAttributes) {
        adminService.updateMemberRole(id, role);
        redirectAttributes.addFlashAttribute("successMessage", "역할이 변경되었습니다.");
        return "redirect:/admin/members";
    }

    @PostMapping("/members/{id}/suspend")
    public String suspendMember(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        adminService.suspendMember(id);
        redirectAttributes.addFlashAttribute("successMessage", "멤버가 정지되었습니다.");
        return "redirect:/admin/members";
    }

    @PostMapping("/members/{id}/unsuspend")
    public String unsuspendMember(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        adminService.unsuspendMember(id);
        redirectAttributes.addFlashAttribute("successMessage", "멤버 정지가 해제되었습니다.");
        return "redirect:/admin/members";
    }

    @GetMapping("/posts")
    public String posts(@RequestParam(defaultValue = "0") int page, Model model) {
        var postPage = adminService.getPosts(
                PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "id")));
        model.addAttribute("posts", postPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", postPage.getTotalPages());
        model.addAttribute("totalElements", postPage.getTotalElements());
        return "admin/posts";
    }

    @PostMapping("/posts/{id}/delete")
    public String deletePost(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        adminService.deletePost(id);
        redirectAttributes.addFlashAttribute("successMessage", "게시글이 삭제되었습니다.");
        return "redirect:/admin/posts";
    }

    @GetMapping("/boards")
    public String boards(@RequestParam(defaultValue = "0") int page, Model model) {
        var boardPage = adminService.getBoards(
                PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "id")));
        model.addAttribute("boards", boardPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", boardPage.getTotalPages());
        model.addAttribute("totalElements", boardPage.getTotalElements());
        return "admin/boards";
    }

    @GetMapping("/universities")
    public String universities(@RequestParam(defaultValue = "0") int page, Model model) {
        var uniPage = adminService.getUniversities(
                PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "id")));
        model.addAttribute("universities", uniPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", uniPage.getTotalPages());
        model.addAttribute("totalElements", uniPage.getTotalElements());
        return "admin/universities";
    }

    // ── 학생/대학 인증 관리 ────────────────────────────────────────────────

    @GetMapping("/verifications")
    public String verifications(@RequestParam(required = false) Long memberId,
                                @RequestParam(required = false, defaultValue = "") String uniQuery,
                                Model model) {
        if (memberId != null) {
            model.addAttribute("memberId", memberId);
            model.addAttribute("verification", adminVerificationService.findByMember(memberId));
        }
        model.addAttribute("uniQuery", uniQuery);
        model.addAttribute("universities", adminService.searchUniversities(uniQuery, 50));
        return "admin/verifications";
    }

    @PostMapping("/verifications")
    public String issueVerification(@RequestParam Long memberId,
                                    @RequestParam Long universityId,
                                    @RequestParam(required = false) String note,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        try {
            adminVerificationService.issueManual(new IssueManualVerificationCommand(
                    memberId, universityId, note, adminName(authentication)));
            redirectAttributes.addFlashAttribute("successMessage", "수동 인증을 발급했습니다.");
        } catch (MemberException | VerificationException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "발급 실패: " + e.getMessage());
        } catch (RuntimeException e) {
            log.error("[AdminVerification] 수동 인증 발급 중 예기치 못한 오류 member={} university={}",
                    memberId, universityId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "발급 중 오류가 발생했습니다. 잠시 후 다시 시도하세요.");
        }
        redirectAttributes.addAttribute("memberId", memberId);
        return "redirect:/admin/verifications";
    }

    @PostMapping("/verifications/{credentialId}/revoke")
    public String revokeVerification(@PathVariable Long credentialId,
                                     @RequestParam Long memberId,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        try {
            adminVerificationService.revoke(credentialId, memberId, adminName(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "자격을 취소했습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "취소 실패: " + e.getMessage());
        } catch (RuntimeException e) {
            log.error("[AdminVerification] 자격 취소 중 예기치 못한 오류 credentialId={} member={}",
                    credentialId, memberId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "취소 중 오류가 발생했습니다. 잠시 후 다시 시도하세요.");
        }
        redirectAttributes.addAttribute("memberId", memberId);
        return "redirect:/admin/verifications";
    }

    private String adminName(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalStateException("관리자 인증 정보가 없습니다. Security 설정을 확인하세요.");
        }
        return authentication.getName();
    }

    @GetMapping("/login")
    public String loginPage() {
        return "admin/login";
    }
}
