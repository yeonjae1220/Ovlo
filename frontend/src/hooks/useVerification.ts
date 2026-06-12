'use client'

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { verificationApi } from '../api/verification'
import type { VerificationErrorCode } from '../api/verification'

const ME_KEY = ['verification', 'me'] as const

/** 본인 인증 현황 (소유자 전용 — enabled로 게이팅). */
export function useMyVerification(enabled = true) {
  return useQuery({
    queryKey: ME_KEY,
    queryFn: () => verificationApi.me(),
    enabled,
    staleTime: 1000 * 60,
  })
}

export function useRequestEmailVerification() {
  return useMutation({
    mutationFn: ({ universityId, schoolEmail }: { universityId: number; schoolEmail: string }) =>
      verificationApi.requestCode(universityId, schoolEmail),
  })
}

export function useConfirmEmailVerification() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (code: string) => verificationApi.confirmCode(code),
    onSuccess: (status) => {
      queryClient.setQueryData(ME_KEY, status)
    },
  })
}

/** axios 에러 → 백엔드 VERIFICATION_* 코드 추출 (없으면 null). */
export function extractVerificationErrorCode(error: unknown): VerificationErrorCode | null {
  if (typeof error !== 'object' || error === null) return null
  const response = (error as { response?: { data?: { code?: string } } }).response
  const code = response?.data?.code
  return code && code.startsWith('VERIFICATION_') ? (code as VerificationErrorCode) : null
}
