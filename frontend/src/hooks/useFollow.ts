import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { followApi } from '../api/follow'

export function useFollowers(memberId: string) {
  return useQuery({
    queryKey: ['followers', memberId],
    queryFn: () => followApi.getFollowers(memberId),
    enabled: !!memberId,
  })
}

export function useFollowings(memberId: string) {
  return useQuery({
    queryKey: ['followings', memberId],
    queryFn: () => followApi.getFollowings(memberId),
    enabled: !!memberId,
  })
}

export function useFollow() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (followeeId: string) => followApi.follow(followeeId),
    onSuccess: (_, followeeId) => {
      queryClient.invalidateQueries({ queryKey: ['followings'] })
      queryClient.invalidateQueries({ queryKey: ['followers', followeeId] })
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
    },
  })
}
