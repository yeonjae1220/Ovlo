'use client'

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { memberApi } from '../api/member'
import { useAuthStore } from '../store/authStore'
import type { UpdateMemberProfileRequest } from '../types'

export function useMember(id: string) {
  return useQuery({
    queryKey: ['member', id],
    queryFn: () => memberApi.getById(id),
    enabled: !!id,
  })
}

export function useMemberSearch(keyword: string) {
  return useQuery({
    queryKey: ['members', 'search', keyword],
    queryFn: () => memberApi.search(keyword),
    enabled: keyword.length >= 1,
  })
}

/** 가입 화면용 — 로그인 전에 부를 수 있고, 같은 닉네임이 정확히 있는지만 알려준다. */
export function useNicknameAvailability(nickname: string) {
  return useQuery({
    queryKey: ['members', 'check-nickname', nickname],
    queryFn: () => memberApi.checkNickname(nickname),
    enabled: nickname.length >= 2,
  })
}

export function useUpdateProfile() {
  const queryClient = useQueryClient()
  const { currentUser, setCurrentUser } = useAuthStore()

  return useMutation({
    mutationFn: ({ id, req }: { id: string; req: UpdateMemberProfileRequest }) =>
      memberApi.updateProfile(id, req),
    onSuccess: (updated) => {
      queryClient.setQueryData(['member', updated.id], updated)
      if (currentUser?.id === updated.id) setCurrentUser(updated)
    },
  })
}
