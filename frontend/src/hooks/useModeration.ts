'use client'

import { useMutation, useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { moderationApi, type ReportRequest } from '../api/moderation'

/**
 * 차단 상태가 바뀌면 서버는 피드·게시글·댓글·검색·팔로우·DM 에서 상대를 감추거나 되살린다.
 * 화면에 남은 캐시가 그 결과와 어긋나지 않도록 관련 쿼리를 모두 다시 가져온다.
 * (중고거래 키는 회원 id 를 둘째 요소로 갖지만 첫 요소 prefix 로 함께 무효화된다.)
 */
const BLOCK_SENSITIVE_QUERY_KEYS = [
  'blocks',
  'block-status',
  'posts',
  'post',
  'boards',
  'board',
  'followers',
  'followings',
  'following-status',
  'members',
  'member',
  'chatRooms',
  'chatRoom',
  'chatMessages',
  'market-blocks',
  'market-list',
  'market-detail',
  'market-translation',
] as const

export function invalidateAfterBlockChange(queryClient: QueryClient): Promise<void> {
  return Promise.all(
    BLOCK_SENSITIVE_QUERY_KEYS.map((key) => queryClient.invalidateQueries({ queryKey: [key] }))
  ).then(() => undefined)
}

export function useBlockStatus(memberId: number | null, enabled: boolean) {
  return useQuery({
    queryKey: ['block-status', memberId],
    queryFn: () => moderationApi.blockStatus(memberId!),
    enabled: enabled && memberId !== null && Number.isFinite(memberId) && memberId > 0,
  })
}

export function useBlockedMembers(before: number | undefined, enabled: boolean) {
  return useQuery({
    queryKey: ['blocks', before ?? null],
    queryFn: () => moderationApi.blockedMembers(before),
    enabled,
  })
}

export function useBlockMember() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (memberId: number) => moderationApi.block(memberId),
    onSuccess: () => invalidateAfterBlockChange(queryClient),
  })
}

export function useUnblockMember() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (memberId: number) => moderationApi.unblock(memberId),
    onSuccess: () => invalidateAfterBlockChange(queryClient),
  })
}

export function useReportContent() {
  return useMutation({
    mutationFn: (req: ReportRequest) => moderationApi.report(req),
  })
}
