'use client'

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { isAxiosError } from 'axios'
import { officeApi } from '../api/office'
import type { ConsultationStatus, InquiryOutcome, NoticeCategory, NoticeLang, NoticeRequest, NoticeStatus } from '../api/office'

/**
 * 내 국제처. 404(소속 없음)는 "없음"이라는 정상 답이므로 null 로, 그 외 실패는 에러로 둔다
 * (GLOBAL-PIT-108: 실패를 빈 값으로 치환하지 않는다).
 */
export function useMyOffice() {
  return useQuery({
    queryKey: ['office', 'me'],
    queryFn: () =>
      officeApi.me().catch((e: unknown) => {
        if (isAxiosError(e) && e.response?.status === 404) return null
        throw e
      }),
  })
}

export function useAcceptOfficeInvitation() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: () => officeApi.acceptInvitation(),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['office'] }),
  })
}

export function useOfficeNotices(officeId: number | undefined, status: NoticeStatus | '', category: NoticeCategory | '', page: number) {
  return useQuery({
    queryKey: ['office', officeId, 'notices', status, category, page],
    queryFn: () => officeApi.listNotices(officeId as number, status, category, page),
    enabled: officeId !== undefined,
  })
}

export function useOfficeNotice(officeId: number | undefined, noticeId: number | undefined) {
  return useQuery({
    queryKey: ['office', officeId, 'notice', noticeId],
    queryFn: () => officeApi.getNotice(officeId as number, noticeId as number),
    enabled: officeId !== undefined && noticeId !== undefined,
  })
}

function useNoticeMutation<TArgs>(officeId: number | undefined, fn: (officeId: number, args: TArgs) => Promise<unknown>) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (args: TArgs) => fn(officeId as number, args),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['office', officeId] }),
  })
}

export function useRegisterNotice(officeId: number | undefined) {
  return useNoticeMutation<NoticeRequest>(officeId, (id, req) => officeApi.registerNotice(id, req))
}

export function useUpdateNotice(officeId: number | undefined) {
  return useNoticeMutation<{ noticeId: number; req: NoticeRequest }>(officeId, (id, a) => officeApi.updateNotice(id, a.noticeId, a.req))
}

export function useApproveNotice(officeId: number | undefined) {
  return useNoticeMutation<number>(officeId, (id, noticeId) => officeApi.approveNotice(id, noticeId))
}

export function useArchiveNotice(officeId: number | undefined) {
  return useNoticeMutation<number>(officeId, (id, noticeId) => officeApi.archiveNotice(id, noticeId))
}

export function useReviewTranslation(officeId: number | undefined) {
  return useNoticeMutation<{ noticeId: number; lang: NoticeLang }>(officeId, (id, a) => officeApi.reviewTranslation(id, a.noticeId, a.lang))
}

export function useRetranslateNotice(officeId: number | undefined) {
  return useNoticeMutation<number>(officeId, (id, noticeId) => officeApi.retranslateNotice(id, noticeId))
}

export function useStudentNotices(officeId: number | undefined, lang: string, category: NoticeCategory | '') {
  return useQuery({
    queryKey: ['office', officeId, 'student-notices', lang, category],
    queryFn: () => officeApi.listStudentNotices(officeId as number, lang, category),
    enabled: officeId !== undefined,
  })
}

export function useAskQuestion(officeId: number | undefined) {
  return useMutation({
    mutationFn: (args: { question: string; lang: string }) => officeApi.ask(officeId as number, args.question, args.lang),
  })
}

export function useInquiryFeedback(officeId: number | undefined) {
  return useMutation({
    mutationFn: (args: { inquiryId: number; helpful: boolean; reason?: string }) =>
      officeApi.inquiryFeedback(officeId as number, args.inquiryId, args.helpful, args.reason),
  })
}

export function useOfficeInquiries(officeId: number | undefined, outcome: InquiryOutcome | '', page: number) {
  return useQuery({
    queryKey: ['office', officeId, 'inquiries', outcome, page],
    queryFn: () => officeApi.listInquiries(officeId as number, outcome, page),
    enabled: officeId !== undefined,
  })
}

// ── 상담 이관 ────────────────────────────────────────────────────────────────

export function useOpenConsultation(officeId: number | undefined) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (body: { inquiryId?: number | null; consentVersion: string; message?: string | null }) =>
      officeApi.openConsultation(officeId as number, body),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['office', officeId, 'my-consultations'] }),
  })
}

export function useMyConsultations(officeId: number | undefined) {
  return useQuery({
    queryKey: ['office', officeId, 'my-consultations'],
    queryFn: () => officeApi.myConsultations(officeId as number),
    enabled: officeId !== undefined,
  })
}

export function useMyConsultation(officeId: number | undefined, id: number | undefined) {
  return useQuery({
    queryKey: ['office', officeId, 'my-consultation', id],
    queryFn: () => officeApi.myConsultation(officeId as number, id as number),
    enabled: officeId !== undefined && id !== undefined,
  })
}

export function useCloseConsultation(officeId: number | undefined) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => officeApi.closeConsultation(officeId as number, id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['office', officeId] }),
  })
}

/** 상담 대화(번역 병기). 열린 상담은 15초마다 다시 읽는다. */
export function useTranscript(officeId: number | undefined, id: number | undefined, lang: string, staff: boolean, active: boolean) {
  return useQuery({
    queryKey: ['office', officeId, staff ? 'staff-transcript' : 'my-transcript', id, lang],
    queryFn: () => (staff ? officeApi.staffTranscript(officeId as number, id as number, lang) : officeApi.myTranscript(officeId as number, id as number, lang)),
    enabled: officeId !== undefined && id !== undefined,
    refetchInterval: active ? 15000 : false,
  })
}

export function useOfficeConsultations(officeId: number | undefined, status: ConsultationStatus | '', page: number) {
  return useQuery({
    queryKey: ['office', officeId, 'consultations', status, page],
    queryFn: () => officeApi.listConsultations(officeId as number, status, page),
    enabled: officeId !== undefined,
  })
}

export function useStaffConsultation(officeId: number | undefined, id: number | undefined) {
  return useQuery({
    queryKey: ['office', officeId, 'consultation', id],
    queryFn: () => officeApi.getStaffConsultation(officeId as number, id as number),
    enabled: officeId !== undefined && id !== undefined,
  })
}

export function useAssignConsultation(officeId: number | undefined) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => officeApi.assignConsultation(officeId as number, id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['office', officeId] }),
  })
}

export function useResolveConsultation(officeId: number | undefined) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => officeApi.resolveConsultation(officeId as number, id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['office', officeId] }),
  })
}
