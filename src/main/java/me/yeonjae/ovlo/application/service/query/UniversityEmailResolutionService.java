package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.UniversityEmailResolution;
import me.yeonjae.ovlo.application.dto.result.UniversityResult;
import me.yeonjae.ovlo.application.port.in.university.ResolveUniversityByEmailQuery;
import me.yeonjae.ovlo.application.port.out.university.UniversityDomainLookupPort;
import me.yeonjae.ovlo.domain.university.model.EmailDomain;
import me.yeonjae.ovlo.domain.university.model.University;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * 학교 이메일 도메인 → 대학 역조회 서비스.
 * 공개 메일 제공자(gmail 등)는 학교 메일이 아니므로 차단하고,
 * 동일 도메인을 가진 대학이 여럿이면 AMBIGUOUS로 반환해 상위(본교)에서 disambiguate 한다.
 */
@Service
@Transactional(readOnly = true)
public class UniversityEmailResolutionService implements ResolveUniversityByEmailQuery {

    /** 학교 메일이 아닌 대표 공개 메일 도메인. */
    private static final Set<String> PUBLIC_PROVIDERS = Set.of(
            "gmail.com", "googlemail.com", "naver.com", "daum.net", "hanmail.net",
            "nate.com", "kakao.com", "yahoo.com", "outlook.com", "hotmail.com",
            "live.com", "icloud.com", "me.com", "proton.me", "protonmail.com",
            "qq.com", "163.com", "126.com"
    );

    private final UniversityDomainLookupPort lookupPort;

    public UniversityEmailResolutionService(UniversityDomainLookupPort lookupPort) {
        this.lookupPort = lookupPort;
    }

    @Override
    public UniversityEmailResolution resolveByEmail(String email) {
        EmailDomain emailDomain = EmailDomain.fromEmail(email);
        String domain = emailDomain.value();

        if (PUBLIC_PROVIDERS.contains(domain)) {
            return UniversityEmailResolution.publicProvider(domain);
        }

        List<University> matches = lookupPort.findByEmailDomain(domain);
        if (matches.isEmpty()) {
            return UniversityEmailResolution.notFound(domain);
        }
        if (matches.size() == 1) {
            return UniversityEmailResolution.matched(domain, UniversityResult.from(matches.get(0)));
        }
        return UniversityEmailResolution.ambiguous(
                domain, matches.stream().map(UniversityResult::from).toList());
    }
}
