package me.yeonjae.ovlo.adapter.in.web;
import me.yeonjae.ovlo.application.port.in.marketplace.MarketplaceUseCase;
import me.yeonjae.ovlo.domain.marketplace.exception.MarketplaceException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Existing admin session authentication and CSRF chain; member JWT ADMIN is not sufficient. */
@Controller @RequestMapping("/admin/marketplace")
public class AdminMarketplacePageController {
    private final MarketplaceUseCase market;
    public AdminMarketplacePageController(MarketplaceUseCase market) {this.market=market;}
    @GetMapping public String page(Model model) { model.addAttribute("reports",market.reports()); return "admin/marketplace"; }
    @PostMapping("/{id}") public String moderate(Authentication auth,@PathVariable long id,@RequestParam long version,@RequestParam boolean hidden,RedirectAttributes flash) {
        try {market.moderate(auth.getName(),id,version,hidden);flash.addFlashAttribute("success","처리했습니다.");}
        catch(MarketplaceException e) {flash.addFlashAttribute("error",e.getMessage());}
        return "redirect:/admin/marketplace";
    }
}
