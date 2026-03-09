import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { useCreatePost } from '../../hooks/usePost'
import { useUploadMedia } from '../../hooks/useMedia'
import { useDropzone } from 'react-dropzone'
import type { MediaFile } from '../../types'

export default function CreatePostPage() {
  const [searchParams] = useSearchParams()
  const boardId = searchParams.get('boardId') ?? ''
  const navigate = useNavigate()
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
      { onSuccess: (post) => navigate(`/posts/${post.id}`) }
    )
  }

  return (
    <div style={{ maxWidth: 700, margin: '0 auto' }}>
      <h1>글쓰기</h1>
      <input
        placeholder="제목"
        value={title}
        onChange={(e) => setTitle(e.target.value)}
        style={{ display: 'block', width: '100%', marginBottom: 12, padding: 8 }}
      />
      <textarea
        placeholder="내용을 입력하세요..."
        value={content}
        onChange={(e) => setContent(e.target.value)}
        rows={12}
        style={{ display: 'block', width: '100%', marginBottom: 12, padding: 8 }}
      />

      <div
        {...getRootProps()}
        style={{
          border: '2px dashed #ccc',
          padding: 16,
          marginBottom: 12,
          borderRadius: 8,
          cursor: 'pointer',
          background: isDragActive ? '#f0f0f0' : 'transparent',
        }}
      >
        <input {...getInputProps()} />
        {isDragActive ? <p>파일을 놓으세요</p> : <p>파일 첨부 (드래그 또는 클릭)</p>}
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
      >
        {createPost.isPending ? '등록 중...' : '게시글 등록'}
      </button>
    </div>
  )
}
