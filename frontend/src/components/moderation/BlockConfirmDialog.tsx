'use client'

import { useId } from 'react'
import { useBlockMember } from '../../hooks/useModeration'
import { useI18n } from '../../i18n/I18nProvider'
import { Button } from '../ui'
import { ModerationSheet } from './ModerationSheet'

/** 차단 전에 무엇이 일어나는지(콘텐츠 숨김·DM 차단·팔로우 해제)를 알리고 확인을 받는다. */
export function BlockConfirmDialog({
  memberId,
  nickname,
  onClose,
  onBlocked,
}: {
  memberId: number
  nickname: string
  onClose: () => void
  onBlocked?: () => void
}) {
  const { t } = useI18n()
  const titleId = useId()
  const block = useBlockMember()

  const confirm = () => {
    if (block.isPending) return
    block.mutate(memberId, {
      onSuccess: () => {
        onBlocked?.()
        onClose()
      },
    })
  }

  return (
    <ModerationSheet titleId={titleId} busy={block.isPending} onClose={onClose}>
      <h2 id={titleId} style={{ margin: '0 0 10px', fontSize: 18, overflowWrap: 'anywhere' }}>
        {t('moderation.block.confirmTitle', { name: nickname })}
      </h2>
      <ul style={{ margin: '0 0 12px', paddingLeft: 20, lineHeight: 1.7, color: 'var(--color-text-secondary)', fontSize: 14 }}>
        <li>{t('moderation.block.effect.content')}</li>
        <li>{t('moderation.block.effect.messages')}</li>
        <li>{t('moderation.block.effect.follows')}</li>
        <li>{t('moderation.block.effect.undo')}</li>
      </ul>
      {block.isError && (
        <p role="alert" style={{ margin: '0 0 8px', color: 'var(--color-danger)', fontSize: 13 }}>
          {t('moderation.block.error')}
        </p>
      )}
      <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
        <Button variant="ghost" onClick={onClose} disabled={block.isPending}>{t('moderation.cancel')}</Button>
        <Button variant="danger" onClick={confirm} disabled={block.isPending}>
          {block.isPending ? t('moderation.block.blocking') : t('moderation.block.confirm')}
        </Button>
      </div>
    </ModerationSheet>
  )
}
