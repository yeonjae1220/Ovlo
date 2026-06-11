package me.yeonjae.ovlo.application.service.query;

import me.yeonjae.ovlo.application.dto.result.UniversityEmailResolution;
import me.yeonjae.ovlo.application.port.out.university.UniversityDomainLookupPort;
import me.yeonjae.ovlo.domain.university.model.CountryCode;
import me.yeonjae.ovlo.domain.university.model.University;
import me.yeonjae.ovlo.domain.university.model.UniversityId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UniversityEmailResolutionServiceTest {

    @Mock
    private UniversityDomainLookupPort lookupPort;

    @InjectMocks
    private UniversityEmailResolutionService service;

    private University university(long id, String name, String domain) {
        return University.restore(new UniversityId(id), name, null, null, null,
                new CountryCode("KR"), "Seoul", null, "https://x", domain);
    }

    @Test
    @DisplayName("도메인이 정확히 한 대학과 매칭되면 MATCHED")
    void matched() {
        given(lookupPort.findByEmailDomain("snu.ac.kr"))
                .willReturn(List.of(university(1L, "Seoul National University", "snu.ac.kr")));

        UniversityEmailResolution result = service.resolveByEmail("alice@snu.ac.kr");

        assertThat(result.status()).isEqualTo(UniversityEmailResolution.Status.MATCHED);
        assertThat(result.candidates()).hasSize(1);
        assertThat(result.candidates().get(0).id()).isEqualTo(1L);
        assertThat(result.isVerifiable()).isTrue();
        assertThat(result.matchesUniversity(1L)).isTrue();
        assertThat(result.matchesUniversity(2L)).isFalse();
    }

    @Test
    @DisplayName("동일 도메인을 가진 대학이 여럿이면 AMBIGUOUS")
    void ambiguous() {
        given(lookupPort.findByEmailDomain("antalya.edu.tr"))
                .willReturn(List.of(
                        university(10L, "Antalya International University", "antalya.edu.tr"),
                        university(11L, "Antalya Bilim University", "antalya.edu.tr")));

        UniversityEmailResolution result = service.resolveByEmail("stu@antalya.edu.tr");

        assertThat(result.status()).isEqualTo(UniversityEmailResolution.Status.AMBIGUOUS);
        assertThat(result.candidates()).hasSize(2);
        assertThat(result.isVerifiable()).isTrue();
        assertThat(result.matchesUniversity(11L)).isTrue();
    }

    @Test
    @DisplayName("카탈로그에 없는 도메인이면 NOT_FOUND")
    void notFound() {
        given(lookupPort.findByEmailDomain("unknown-school.edu")).willReturn(List.of());

        UniversityEmailResolution result = service.resolveByEmail("x@unknown-school.edu");

        assertThat(result.status()).isEqualTo(UniversityEmailResolution.Status.NOT_FOUND);
        assertThat(result.candidates()).isEmpty();
        assertThat(result.isVerifiable()).isFalse();
    }

    @Test
    @DisplayName("공개 메일(gmail 등)은 조회 없이 PUBLIC_PROVIDER")
    void publicProvider() {
        UniversityEmailResolution result = service.resolveByEmail("someone@gmail.com");

        assertThat(result.status()).isEqualTo(UniversityEmailResolution.Status.PUBLIC_PROVIDER);
        assertThat(result.isVerifiable()).isFalse();
        // 공개 도메인은 DB 조회조차 하지 않는다
        org.mockito.Mockito.verifyNoInteractions(lookupPort);
    }

    @Test
    @DisplayName("잘못된 이메일은 예외가 발생한다")
    void invalidEmail() {
        assertThatThrownBy(() -> service.resolveByEmail("not-an-email"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
