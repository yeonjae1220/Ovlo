import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useBoards, useCreateBoard } from '../../hooks/useBoard'
import type { CreateBoardRequest, BoardCategory, LocationScope } from '../../types'

export default function BoardListPage() {
  const { data: boards, isLoading } = useBoards()
  const createBoard = useCreateBoard()
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState<CreateBoardRequest>({
    name: '',
    category: 'GENERAL',
    scope: 'GLOBAL',
  })

  const handleCreate = () => {
    createBoard.mutate(form, { onSuccess: () => setShowForm(false) })
  }

  if (isLoading) return <p>로딩 중...</p>

  return (
    <div style={{ maxWidth: 800, margin: '0 auto' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1>게시판</h1>
        <button onClick={() => setShowForm(!showForm)}>+ 게시판 만들기</button>
      </div>

      {showForm && (
        <div style={{ border: '1px solid #ddd', padding: 16, marginBottom: 16, borderRadius: 8 }}>
          <input
            placeholder="게시판 이름"
            value={form.name}
            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
            style={{ display: 'block', width: '100%', marginBottom: 8 }}
          />
          <input
            placeholder="설명 (선택)"
            value={form.description ?? ''}
            onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
            style={{ display: 'block', width: '100%', marginBottom: 8 }}
          />
          <select
            value={form.category}
            onChange={(e) => setForm((f) => ({ ...f, category: e.target.value as BoardCategory }))}
            style={{ marginRight: 8 }}
          >
            {['GENERAL', 'LANGUAGE_EXCHANGE', 'TRADE', 'CULTURE', 'HOUSING', 'ACADEMIC', 'JOB'].map(
              (c) => <option key={c} value={c}>{c}</option>
            )}
          </select>
          <select
            value={form.scope}
            onChange={(e) => setForm((f) => ({ ...f, scope: e.target.value as LocationScope }))}
          >
            {['GLOBAL', 'COUNTRY', 'REGION', 'UNIVERSITY'].map((s) => <option key={s} value={s}>{s}</option>)}
          </select>
          <div style={{ marginTop: 8 }}>
            <button onClick={handleCreate} disabled={createBoard.isPending}>생성</button>
            <button onClick={() => setShowForm(false)} style={{ marginLeft: 8 }}>취소</button>
          </div>
        </div>
      )}

      <ul style={{ listStyle: 'none', padding: 0 }}>
        {boards?.map((board) => (
          <li key={board.id} style={{ padding: 12, borderBottom: '1px solid #eee' }}>
            <Link to={`/boards/${board.id}`} style={{ fontWeight: 'bold', fontSize: 16 }}>
              {board.name}
            </Link>
            <span style={{ marginLeft: 8, color: '#888', fontSize: 13 }}>
              {board.category} · {board.scope}
            </span>
            {board.description && <p style={{ margin: '4px 0 0', color: '#555' }}>{board.description}</p>}
          </li>
        ))}
      </ul>
    </div>
  )
}
