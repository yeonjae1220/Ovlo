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
