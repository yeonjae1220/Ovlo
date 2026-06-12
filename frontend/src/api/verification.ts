import apiClient from '../utils/axios'

/** 신뢰 등급 — 백엔드 TrustLevel enum과 1:1. UNVERIFIED < STUDENT < EXCHANGE_VERIFIED. */
export type TrustLevel = 'UNVERIFIED' | 'STUDENT' | 'EXCHANGE_VERIFIED'

/** 백엔드 VerificationException.ErrorType + "VERIFICATION_" 접두 코드. */
export type VerificationErrorCode =
  | 'VERIFICATION_DOMAIN_MISMATCH'
  | 'VERIFICATION_UNIVERSITY_NOT_RESOLVED'
  | 'VERIFICATION_PUBLIC_PROVIDER'
  | 'VERIFICATION_CHALLENGE_NOT_FOUND'
  | 'VERIFICATION_CODE_MISMATCH'
  | 'VERIFICATION_CODE_EXPIRED'
  | 'VERIFICATION_TOO_MANY_ATTEMPTS'
  | 'VERIFICATION_EMAIL_ALREADY_USED'
  | 'VERIFICATION_RATE_LIMITED'

export interface VerifiedUniversity {
  universityId: number
  verifiedEmail: string
  verifiedAt: string
}

/** GET /verification/me — 본인 신뢰 등급 + 인증 대학 목록. */
export interface VerificationStatus {
  trustLevel: TrustLevel
  verifiedUniversities: VerifiedUniversity[]
}

/** POST /verification/email/request 결과 (코드 자체는 응답에 없음). */
export interface VerificationRequestResult {
  maskedEmail: string
  expiresInSeconds: number
}

export const verificationApi = {
  /** 지정 대학의 학교 이메일로 6자리 코드 발송. */
  requestCode: (universityId: number, schoolEmail: string) =>
    apiClient
      .post<VerificationRequestResult>('/verification/email/request', { universityId, schoolEmail })
      .then((r) => r.data),

  /** 발송된 코드를 확인해 인증 자격 발급 → 갱신된 현황 반환. */
  confirmCode: (code: string) =>
    apiClient
      .post<VerificationStatus>('/verification/email/confirm', { code })
      .then((r) => r.data),

  /** 본인 인증 현황. */
  me: () =>
    apiClient.get<VerificationStatus>('/verification/me').then((r) => r.data),
}
