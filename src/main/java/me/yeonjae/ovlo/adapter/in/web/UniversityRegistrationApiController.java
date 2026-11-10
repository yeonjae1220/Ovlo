package me.yeonjae.ovlo.adapter.in.web;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import me.yeonjae.ovlo.adapter.in.web.dto.request.UniversityRegistrationRequest;
import me.yeonjae.ovlo.application.dto.result.MemberResult;
import me.yeonjae.ovlo.application.port.in.calendar.UniversityApplicationUseCase;
import me.yeonjae.ovlo.shared.security.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class UniversityRegistrationApiController {
    private final UniversityApplicationUseCase applications;
    private final RateLimiterService rateLimiter;
    private final ClientIpResolver ips;
    public UniversityRegistrationApiController(UniversityApplicationUseCase applications, RateLimiterService rateLimiter, ClientIpResolver ips) {
        this.applications = applications; this.rateLimiter = rateLimiter; this.ips = ips;
    }
    @PostMapping("/api/v1/members/university-registration") @ResponseStatus(HttpStatus.CREATED)
    public MemberResult register(@Valid @RequestBody UniversityRegistrationRequest r, HttpServletRequest request) {
        rateLimiter.checkSignupRate(ips.resolve(request));
        return applications.register(r.command());
    }
}
