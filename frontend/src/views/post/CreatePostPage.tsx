'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useSearchParams } from 'next/navigation'
import { useCreatePost } from '../../hooks/usePost'
import { useUploadMedia } from '../../hooks/useMedia'
import { useDropzone } from 'react-dropzone'
import type { MediaFile } from '../../types'
import { useI18n } from '../../i18n/I18nProvider'

const C = {
  border: 'var(--color-border-strong)',
  surface: 'var(--color-surface)',
  surfaceSoft: 'var(--color-surface-soft)',
  text: 'var(--color-text)',
  muted: 'var(--color-text-muted)',
  accent: 'var(--color-accent-strong)',
}

export default function CreatePostPage() {
  const { t } = useI18n()
  const searchParams = useSearchParams()
  const boardId = searchParams?.get('boardId') ?? ''
  const router = useRouter()
  const createPost = useCreatePost()
  const uploadMedia = useUploadMedia()

  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [uploadedMedia, setUploadedMedia] = useState<MediaFile[]>([])

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop: async (files) => {
      for (const file of files) {
        const media = await uploadMedia.mutateAsync(file)
        setUploadedMedia((prev) => [...prev, media])
      }
    },
  })

  const handleSubmit = () => {
    if (!title || !content || !boardId) return
    createPost.mutate(
      { boardId: Number(boardId), title, content },
      { onSuccess: (post) => router.push(`/posts/${post.id}`) }
    )
  }

  return (
    <div>
      <h1 style={{ color: C.text }}>{t('post.write.title')}</h1>
      <input
        placeholder={t('post.write.titlePlaceholder')}
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        style={{ display: 'block', width: '100%', marginBottom: 12, padding: 10 }}
      />
      <textarea
        placeholder={t('post.write.contentPlaceholder')}
        value={content}
        onChange={(e) => setContent(e.target.value)}
        rows={12}
        style={{ display: 'block', width: '100%', marginBottom: 12, padding: 10, resize: 'vertical' }}
      />

      <div
        {...getRootProps()}
        style={{
          border: `2px dashed ${C.border}`, padding: 16, marginBottom: 12,
          borderRadius: 8, cursor: 'pointer',
          background: isDragActive ? C.surfaceSoft : C.surface,
          color: C.muted,
        }}
      >
        <input {...getInputProps()} />
        {isDragActive ? <p>{t('post.write.dropActive')}</p> : <p>{t('post.write.dropIdle')}</p>}
      </div>

      {uploadedMedia.length > 0 && (
        <ul style={{ marginBottom: 12 }}>
          {uploadedMedia.map((m) => (
            <li key={m.mediaId}>{m.originalFilename}</li>
          ))}
        </ul>
      )}

      <button
        onClick={handleSubmit}
        disabled={createPost.isPending || !title || !content}
        style={{ background: C.accent, color: '#fff', border: 'none', fontWeight: 700 }}
      >
        {createPost.isPending ? t('post.write.submitting') : t('post.write.submit')}
      </button>
    </div>
  )
}
