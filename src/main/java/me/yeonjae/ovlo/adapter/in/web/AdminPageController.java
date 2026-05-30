package me.yeonjae.ovlo.adapter.in.web;

import lombok.RequiredArgsConstructor;
import me.yeonjae.ovlo.adapter.in.web.dto.response.AdminMemberResponse;
import me.yeonjae.ovlo.application.service.admin.AdminService;
import me.yeonjae.ovlo.domain.member.model.MemberRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminPageController {

    private final AdminService adminService;

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

    @GetMapping("/login")
    public String loginPage() {
        return "admin/login";
    }
}
