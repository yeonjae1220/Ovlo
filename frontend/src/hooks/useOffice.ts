'use client'

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { isAxiosError } from 'axios'
import { officeApi } from '../api/office'
import type { NoticeCategory, NoticeLang, NoticeRequest, NoticeStatus } from '../api/office'

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
