package me.yeonjae.ovlo.adapter.in.web;

import me.yeonjae.ovlo.application.port.in.moderation.ModerateReportUseCase;
import me.yeonjae.ovlo.domain.moderation.exception.ModerationException;
import me.yeonjae.ovlo.domain.moderation.model.ReportResolution;
import me.yeonjae.ovlo.domain.moderation.model.ReportStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 게시글·댓글·채팅·회원 신고 처리 (운영 정책상 24시간 안에 처리). */
@Controller
@RequestMapping("/admin/reports")
public class AdminReportPageController {

    private final ModerateReportUseCase moderateReportUseCase;

    public AdminReportPageController(ModerateReportUseCase moderateReportUseCase) {
        this.moderateReportUseCase = moderateReportUseCase;
    }

    @GetMapping
    public String reports(@RequestParam(defaultValue = "OPEN") ReportStatus status, Model model) {
        model.addAttribute("reports", moderateReportUseCase.reports(status));
        model.addAttribute("status", status);
        return "admin/reports";
    }

    @PostMapping("/{id}/resolve")
    public String resolve(Authentication auth, @PathVariable long id, @RequestParam ReportResolution resolution,
                          RedirectAttributes flash) {
        try {
            moderateReportUseCase.resolve(auth.getName(), id, resolution);
            flash.addFlashAttribute("successMessage", "신고를 처리했습니다.");
        } catch (ModerationException e) {
            flash.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/reports";
    }
}
