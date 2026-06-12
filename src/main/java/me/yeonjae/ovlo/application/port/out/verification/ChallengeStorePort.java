package me.yeonjae.ovlo.application.port.out.verification;

import me.yeonjae.ovlo.domain.verification.model.EmailVerificationChallenge;

import java.time.Duration;
import java.util.Optional;

/** 진행 중인 이메일 인증 챌린지 저장소 (Redis, TTL 기반 자동 만료). */
public interface ChallengeStorePort {
    void save(EmailVerificationChallenge challenge, Duration ttl);
    Optional<EmailVerificationChallenge> findByMemberId(Long memberId);
    void deleteByMemberId(Long memberId);
}
