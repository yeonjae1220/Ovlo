'use client'

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { followApi } from '../api/follow'

export function useFollowers(memberId: string, page = 0, size = 20) {
  return useQuery({
    queryKey: ['followers', memberId, page, size],
    queryFn: () => followApi.getFollowers(memberId, page, size),
    enabled: !!memberId,
  })
}

export function useFollowings(memberId: string, page = 0, size = 20) {
  return useQuery({
    queryKey: ['followings', memberId, page, size],
    queryFn: () => followApi.getFollowings(memberId, page, size),
    enabled: !!memberId,
  })
}

/**
 * 후보 회원 중 내가 팔로우 중인 ID 집합.
 * 목록 API 는 페이지 단위라 "팔로우 중인가" 판정에 쓰면 첫 페이지 밖을 놓친다.
 */
export function useFollowingStatus(memberIds: string[]) {
  const ids = [...new Set(memberIds.filter(Boolean))].sort()
  return useQuery({
    queryKey: ['following-status', ids],
    queryFn: () => followApi.getFollowingStatus(ids),
    enabled: ids.length > 0,
    select: (data) => new Set(data.following.map(String)),
  })
}

export function useFollow() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (followeeId: string) => followApi.follow(followeeId),
    onSuccess: (_, followeeId) => {
      queryClient.invalidateQueries({ queryKey: ['followings'] })
      queryClient.invalidateQueries({ queryKey: ['followers', followeeId] })
      queryClient.invalidateQueries({ queryKey: ['following-status'] })
    },
  })
}

export function useUnfollow() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (followeeId: string) => followApi.unfollow(followeeId),
    onSuccess: (_, followeeId) => {
      queryClient.invalidateQueries({ queryKey: ['followings'] })
      queryClient.invalidateQueries({ queryKey: ['followers', followeeId] })
      queryClient.invalidateQueries({ queryKey: ['following-status'] })
    },
  })
}
