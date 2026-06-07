'use client'

import { useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { useCreatePost } from '../../hooks/usePost'
import { useI18n } from '../../i18n/I18nProvider'
import { Button, Card, EmptyState, FieldGroup, PageHeader, TextAreaField, TextField } from '../../components/ui'

const C = {
  dim: 'var(--color-text-dim)',
  accent: 'var(--color-accent)',
}

export default function CreatePostPage() {
  const { t } = useI18n()
  const searchParams = useSearchParams()
  const boardId = searchParams?.get('boardId') ?? ''
  const router = useRouter()
  const createPost = useCreatePost()

  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')

  const disabledReason = !boardId
    ? t('post.write.noBoard')
    : !title.trim()
      ? t('post.write.titlePlaceholder')
      : !content.trim()
        ? t('post.write.contentPlaceholder')
        : ''

  const handleSubmit = () => {
    if (disabledReason) return
    createPost.mutate(
      { boardId: Number(boardId), title: title.trim(), content: content.trim() },
      { onSuccess: (post) => router.push(`/posts/${post.id}`) }
    )
  }

  return (
    <div style={{ maxWidth: 820, margin: '0 auto' }}>
      <PageHeader
        title={t('post.write.title')}
        description={t('post.write.description')}
        actions={<Button variant="ghost" onClick={() => router.back()} icon="←">{t('exch.back').replace('← ', '')}</Button>}
      />

      {!boardId ? (
        <EmptyState icon="!" title={t('post.write.noBoard')} description={t('post.write.noBoardDesc')} />
      ) : (
        <Card style={{ padding: 18 }}>
          <div style={{ display: 'grid', gap: 16 }}>
            <FieldGroup label={t('post.write.titlePlaceholder')} hint={`${title.length}/80`}>
              <TextField
                placeholder={t('post.write.titleExample')}
                value={title}
                maxLength={80}
                onChange={(event) => setTitle(event.target.value)}
              />
            </FieldGroup>

            <FieldGroup label={t('post.write.contentPlaceholder')} hint={t('post.write.contentLength', { count: content.length })}>
              <TextAreaField
                placeholder={t('post.write.contentHint')}
                value={content}
                onChange={(event) => setContent(event.target.value)}
                rows={12}
              />
            </FieldGroup>

            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 12, flexWrap: 'wrap' }}>
              <span style={{ color: disabledReason ? C.dim : C.accent, fontSize: 13, fontWeight: 750 }}>
                {disabledReason ? `${disabledReason}` : t('post.write.ready')}
              </span>
              <Button
                onClick={handleSubmit}
                disabled={createPost.isPending || !!disabledReason}
                variant="primary"
              >
                {createPost.isPending ? t('post.write.submitting') : t('post.write.submit')}
              </Button>
            </div>
          </div>
        </Card>
      )}
    </div>
  )
}
