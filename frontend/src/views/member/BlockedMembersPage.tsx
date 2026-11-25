'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useAuthStore } from '../../store/authStore'
import { useBlockedMembers, useUnblockMember } from '../../hooks/useModeration'
import { useI18n } from '../../i18n/I18nProvider'
import { Avatar, Button, Card, EmptyState, PageHeader, QueryErrorNotice } from '../../components/ui'

/**
 * 내가 차단한 회원 목록(서비스 전체 차단). 중고거래 차단도 같은 테이블을 쓰므로
 * /marketplace/blocks 도 이 화면을 그린다. 상대가 나를 차단한 것은 여기서 풀 수 없다.
 */
export default function BlockedMembersPage() {
  const user = useAuthStore((state) => state.currentUser?.id)
  // 계정이 바뀌면 커서 상태를 버린다
  return <BlockedMemberList key={user} enabled={!!user} />
}

function BlockedMemberList({ enabled }: { enabled: boolean }) {
  const { t } = useI18n()
  const router = useRouter()
  const [cursors, setCursors] = useState<(number | undefined)[]>([undefined])
  const before = cursors[cursors.length - 1]
  const result = useBlockedMembers(before, enabled)
  const unblock = useUnblockMember()

  return (
    <div style={{ maxWidth: 720, margin: '0 auto' }}>
      <PageHeader
        title={t('moderation.blocks.title')}
        description={t('moderation.blocks.hint')}
        actions={<Button variant="ghost" onClick={() => router.back()} icon="←">{t('exch.back').replace('← ', '')}</Button>}
      />

      {unblock.isError && (
        <p role="alert" style={{ color: 'var(--color-danger)', fontSize: 13 }}>{t('moderation.unblock.error')}</p>
      )}

      {result.isPending ? (
        <p style={{ color: 'var(--color-text-muted)' }}>{t('common.loading')}</p>
      ) : result.isError ? (
        <QueryErrorNotice onRetry={() => void result.refetch()} />
      ) : (
        <>
          {result.data.content.length === 0 ? (
            <EmptyState icon="∅" title={t('moderation.blocks.empty')} />
          ) : (
            <Card style={{ padding: '4px 16px' }}>
              <ul style={{ listStyle: 'none', margin: 0, padding: 0 }}>
                {result.data.content.map((member) => (
                  <li
                    key={member.memberId}
                    style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '12px 0', borderBottom: '1px solid var(--color-border)' }}
                  >
                    <Avatar label={member.nickname} size="sm" />
                    <Link
                      href={`/profile/${member.memberId}`}
                      style={{ flex: 1, minWidth: 0, color: 'var(--color-text)', fontWeight: 700, overflowWrap: 'anywhere', textDecoration: 'none' }}
                    >
                      {member.nickname}
                    </Link>
                    <Button
                      variant="secondary"
                      disabled={unblock.isPending}
                      onClick={() => unblock.mutate(member.memberId)}
                      aria-label={t('moderation.blocks.unblockAria', { name: member.nickname })}
                    >
                      {t('moderation.block.unblock')}
                    </Button>
                  </li>
                ))}
              </ul>
            </Card>
          )}
          <div style={{ display: 'flex', justifyContent: 'center', gap: 12, marginTop: 16 }}>
            <Button
              variant="ghost"
              disabled={cursors.length === 1 || unblock.isPending}
              onClick={() => setCursors((values) => values.slice(0, -1))}
            >
              {t('common.prev')}
            </Button>
            <Button
              variant="ghost"
              disabled={result.data.nextBefore === null || unblock.isPending}
              onClick={() => {
                const next = result.data.nextBefore
                if (next !== null) setCursors((values) => [...values, next])
              }}
            >
              {t('common.next')}
            </Button>
          </div>
        </>
      )}
    </div>
  )
}
