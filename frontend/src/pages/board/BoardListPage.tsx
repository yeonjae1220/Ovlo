import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useBoards, useCreateBoard } from '../../hooks/useBoard'
import { usePosts, useAllPosts } from '../../hooks/usePost'
import { useUniversityReports } from '../../hooks/useUniversity'
import { useAuthStore } from '../../store/authStore'
import type { CreateBoardRequest, BoardCategory, LocationScope } from '../../types'

const C = {
  bg:          '#242424',
  card:        '#1e2836',
  border:      '#2d3748',
  borderLight: '#374151',
  textPrimary: '#f1f5f9',
  textSec:     '#cbd5e1',
  textMuted:   '#94a3b8',
  textDim:     '#64748b',
  activeBg:    '#1e3a5f',
  activeBorder:'#2563eb',
  activeText:  '#60a5fa',
  purple:      '#a78bfa',
}

type TabId = 'all' | 'free' | 'reports'

const TABS: { id: TabId; label: string }[] = [
  { id: 'all',     label: '전체' },
  { id: 'free',    label: '자유' },
  { id: 'reports', label: '팁' },
]

export default function BoardListPage() {
  const [activeTab, setActiveTab] = useState<TabId>('all')
  const [allPage, setAllPage] = useState(0)
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [form, setForm] = useState<CreateBoardRequest>({ name: '', category: 'GENERAL', scope: 'GLOBAL' })

  const { currentUser } = useAuthStore()
  const navigate = useNavigate()
  const { data: boards } = useBoards()
  const createBoard = useCreateBoard()

  // 자유게시판: GENERAL+GLOBAL 첫 번째 보드 자동 선택
  const freeBoard = boards?.find((b) => b.category === 'GENERAL' && b.scope === 'GLOBAL') ?? null

  const { data: allPostsPage, isLoading: allPostsLoading } = useAllPosts(allPage, 20)
  const { data: tipsPage, isLoading: tipsLoading } = useUniversityReports('ko', '', 0, 20)
  const { data: freePosts, isLoading: freeLoading } = usePosts(freeBoard?.id ? String(freeBoard.id) : '')
  const { data: reportsPage, isLoading: reportsLoading } = useUniversityReports('ko', '', 0, 20)

  // 전체 탭: 게시글 + 팁을 createdAt 기준 최신순 병합
  type FeedItem =
    | { type: 'post'; data: import('../../types').Post }
    | { type: 'tip'; data: import('../../api/university').UniversityReportSummary }

  const allLoading = allPostsLoading || tipsLoading
  const allFeedItems: FeedItem[] = (() => {
    const posts = (allPostsPage?.content ?? []).map((p): FeedItem => ({ type: 'post', data: p }))
    const tips = (tipsPage?.content ?? []).map((r): FeedItem => ({ type: 'tip', data: r }))
    return [...posts, ...tips].sort((a, b) => {
      const dateA = a.type === 'post' ? a.data.createdAt : a.data.createdAt
      const dateB = b.type === 'post' ? b.data.createdAt : b.data.createdAt
      if (!dateA || !dateB) return 0
      return dateB.localeCompare(dateA)
    })
  })()

  const handleCreate = () => {
    createBoard.mutate(form, { onSuccess: () => { setShowCreateForm(false); setForm({ name: '', category: 'GENERAL', scope: 'GLOBAL' }) } })
  }

  return (
    <div style={{ maxWidth: 860, margin: '0 auto' }}>
      {/* 헤더 */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4 }}>
        <h1 style={{ margin: 0, fontSize: 22, fontWeight: 800, color: C.textPrimary }}>커뮤니티</h1>
        <div style={{ display: 'flex', gap: 8 }}>
          {currentUser && activeTab !== 'reports' && freeBoard && (
            <button
              onClick={() => navigate(`/posts/new?boardId=${freeBoard.id}`)}
              style={{
                padding: '7px 14px', borderRadius: 8, fontSize: 13, cursor: 'pointer',
                border: 'none', background: C.purple, color: '#fff', fontWeight: 600,
              }}
            >
              + 글쓰기
            </button>
          )}
          <button
            onClick={() => setShowCreateForm((v) => !v)}
            style={{
              padding: '7px 12px', borderRadius: 8, fontSize: 12, cursor: 'pointer',
              border: `1px solid ${C.borderLight}`, background: 'transparent', color: C.textDim,
            }}
          >
            {showCreateForm ? '▲ 게시판 만들기 닫기' : '▼ 게시판 만들기'}
          </button>
        </div>
      </div>

      {/* 게시판 생성 폼 */}
      {showCreateForm && (
        <div style={{ border: `1px solid ${C.borderLight}`, padding: 16, marginBottom: 16, borderRadius: 10, background: C.card }}>
          <input
            placeholder="게시판 이름"
            value={form.name}
            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
            style={{ display: 'block', width: '100%', marginBottom: 8, padding: '8px 12px', borderRadius: 6, border: `1px solid ${C.borderLight}`, background: C.bg, color: C.textPrimary, fontSize: 14, boxSizing: 'border-box' }}
          />
          <input
            placeholder="설명 (선택)"
            value={form.description ?? ''}
            onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
            style={{ display: 'block', width: '100%', marginBottom: 8, padding: '8px 12px', borderRadius: 6, border: `1px solid ${C.borderLight}`, background: C.bg, color: C.textPrimary, fontSize: 14, boxSizing: 'border-box' }}
          />
          <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
            <select
              value={form.category}
              onChange={(e) => setForm((f) => ({ ...f, category: e.target.value as BoardCategory }))}
              style={{ padding: '7px 10px', borderRadius: 6, border: `1px solid ${C.borderLight}`, background: C.bg, color: C.textSec, fontSize: 13 }}
            >
              {(['GENERAL','LANGUAGE_EXCHANGE','TRADE','CULTURE','HOUSING','ACADEMIC','JOB'] as BoardCategory[]).map(
                (c) => <option key={c} value={c}>{c}</option>
              )}
            </select>
            <select
              value={form.scope}
              onChange={(e) => setForm((f) => ({ ...f, scope: e.target.value as LocationScope }))}
              style={{ padding: '7px 10px', borderRadius: 6, border: `1px solid ${C.borderLight}`, background: C.bg, color: C.textSec, fontSize: 13 }}
            >
              {(['GLOBAL','COUNTRY','REGION','UNIVERSITY'] as LocationScope[]).map(
                (s) => <option key={s} value={s}>{s}</option>
              )}
            </select>
            <button
              onClick={handleCreate}
              disabled={createBoard.isPending || !form.name.trim()}
              style={{ padding: '7px 16px', borderRadius: 6, border: 'none', background: C.purple, color: '#fff', fontSize: 13, cursor: createBoard.isPending ? 'wait' : 'pointer', fontWeight: 600 }}
            >
              생성
            </button>
            <button
              onClick={() => setShowCreateForm(false)}
              style={{ padding: '7px 12px', borderRadius: 6, border: `1px solid ${C.borderLight}`, background: 'transparent', color: C.textMuted, fontSize: 13, cursor: 'pointer' }}
            >
              취소
            </button>
          </div>
        </div>
      )}

      {/* 탭 바 */}
      <div style={{ display: 'flex', borderBottom: `1px solid ${C.border}`, marginBottom: 0 }}>
        {TABS.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            style={{
              padding: '10px 20px', border: 'none', cursor: 'pointer', fontSize: 14, fontWeight: activeTab === tab.id ? 700 : 400,
              background: 'transparent',
              color: activeTab === tab.id ? C.purple : C.textMuted,
              borderBottom: activeTab === tab.id ? `2px solid ${C.purple}` : '2px solid transparent',
              marginBottom: -1,
              transition: 'color 0.15s',
            }}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* 전체 탭 */}
      {activeTab === 'all' && (
        <div>
          {allLoading && <p style={{ color: C.textMuted, padding: '24px 0' }}>불러오는 중...</p>}
          {!allLoading && allFeedItems.length === 0 && (
            <p style={{ color: C.textDim, padding: '40px 0', textAlign: 'center' }}>아직 게시글이 없습니다.</p>
          )}
          {/* 테이블 헤더 */}
          {allFeedItems.length > 0 && (
            <div style={{
              display: 'grid', gridTemplateColumns: '60px 1fr 56px',
              gap: 4, padding: '8px 4px', borderBottom: `1px solid ${C.border}`,
              fontSize: 12, color: C.textDim, fontWeight: 600,
            }}>
              <span>번호</span>
              <span>제목</span>
              <span style={{ textAlign: 'center' }}>👍</span>
            </div>
          )}
          {allFeedItems.map((item, idx) => {
            const total = allFeedItems.length
            const num = total - idx
            if (item.type === 'post') {
              const post = item.data
              return (
                <Link key={`post-${post.id}`} to={`/posts/${post.id}`} style={{ textDecoration: 'none', color: 'inherit' }}>
                  <div
                    style={{
                      display: 'grid', gridTemplateColumns: '60px 1fr 56px',
                      gap: 4, padding: '10px 4px', borderBottom: `1px solid ${C.border}`,
                      alignItems: 'center', cursor: 'pointer', transition: 'background 0.12s',
                    }}
                    onMouseEnter={(e) => { (e.currentTarget as HTMLDivElement).style.background = C.card }}
                    onMouseLeave={(e) => { (e.currentTarget as HTMLDivElement).style.background = 'transparent' }}
                  >
                    <span style={{ fontSize: 12, color: C.textDim }}>{num}</span>
                    <span>
                      {post.deleted ? (
                        <span style={{ fontSize: 14, color: C.textDim }}>[삭제된 게시글]</span>
                      ) : (
                        <>
                          <span style={{ fontSize: 14, color: C.textPrimary, fontWeight: 500 }}>{post.title}</span>
                          {post.boardName && (
                            <span style={{ marginLeft: 8, fontSize: 11, color: C.activeText, background: '#1e3a5f', padding: '2px 6px', borderRadius: 4 }}>
                              {post.boardName}
                            </span>
                          )}
                        </>
                      )}
                    </span>
                    <span style={{ fontSize: 12, color: post.likeCount > 0 ? '#f59e0b' : C.textDim, textAlign: 'center' }}>
                      {post.likeCount > 0 ? post.likeCount : ''}
                    </span>
                  </div>
                </Link>
              )
            } else {
              const tip = item.data
              return (
                <div
                  key={`tip-${tip.id}`}
                  onClick={() => navigate(`/university-reports/${tip.id}?lang=ko`)}
                  style={{
                    display: 'grid', gridTemplateColumns: '60px 1fr 56px',
                    gap: 4, padding: '10px 4px', borderBottom: `1px solid ${C.border}`,
                    alignItems: 'center', cursor: 'pointer', transition: 'background 0.12s',
                  }}
                  onMouseEnter={(e) => { (e.currentTarget as HTMLDivElement).style.background = C.card }}
                  onMouseLeave={(e) => { (e.currentTarget as HTMLDivElement).style.background = 'transparent' }}
                >
                  <span style={{ fontSize: 12, color: C.textDim }}>{num}</span>
                  <span>
                    <span style={{ fontSize: 14, color: C.textPrimary, fontWeight: 500 }}>{tip.title}</span>
                    <span style={{ marginLeft: 8, fontSize: 11, color: C.purple, background: '#2e1a5f', padding: '2px 6px', borderRadius: 4 }}>
                      팁
                    </span>
                  </span>
                  <span style={{ fontSize: 12, color: C.textDim, textAlign: 'center' }}></span>
                </div>
              )
            }
          })}
          {/* 페이지네이션 (post 기준) */}
          {(allPostsPage?.totalElements ?? 0) > 20 && (
            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 16, marginTop: 20 }}>
              <button
                onClick={() => setAllPage((p) => p - 1)}
                disabled={allPage === 0}
                style={{ padding: '7px 18px', borderRadius: 8, border: `1px solid ${C.border}`, background: allPage === 0 ? '#1a2234' : C.card, color: allPage === 0 ? '#475569' : C.textSec, cursor: allPage === 0 ? 'default' : 'pointer', fontSize: 13 }}
              >← 이전</button>
              <span style={{ color: C.textMuted, fontSize: 13 }}>{allPage + 1}페이지</span>
              <button
                onClick={() => setAllPage((p) => p + 1)}
                disabled={!(allPostsPage?.hasNext ?? false)}
                style={{ padding: '7px 18px', borderRadius: 8, border: `1px solid ${C.border}`, background: !(allPostsPage?.hasNext) ? '#1a2234' : C.card, color: !(allPostsPage?.hasNext) ? '#475569' : C.textSec, cursor: !(allPostsPage?.hasNext) ? 'default' : 'pointer', fontSize: 13 }}
              >다음 →</button>
            </div>
          )}
        </div>
      )}

      {/* 자유 탭 */}
      {activeTab === 'free' && (
        <>
          {!freeBoard && !freeLoading && (
            <div style={{ padding: '40px 0', textAlign: 'center', color: C.textDim }}>
              <p style={{ marginBottom: 12 }}>자유게시판이 아직 없습니다.</p>
              <button
                onClick={() => setShowCreateForm(true)}
                style={{ padding: '8px 18px', borderRadius: 8, border: `1px solid ${C.borderLight}`, background: C.card, color: C.activeText, fontSize: 13, cursor: 'pointer' }}
              >
                게시판 만들기
              </button>
            </div>
          )}
          {freeBoard && (
            <PostTable
              posts={freePosts ?? []}
              isLoading={freeLoading}
              showBoardName={false}
              emptyText="아직 게시글이 없습니다."
              page={0}
              hasNext={false}
              totalElements={freePosts?.length ?? 0}
              onPrev={() => {}}
              onNext={() => {}}
            />
          )}
        </>
      )}

      {/* AI 보고서 탭 */}
      {activeTab === 'reports' && (
        <div>
          {reportsLoading && <p style={{ color: C.textMuted, padding: '24px 0' }}>불러오는 중...</p>}
          {!reportsLoading && (reportsPage?.content ?? []).length === 0 && (
            <p style={{ color: C.textDim, padding: '40px 0', textAlign: 'center' }}>아직 팁이 없습니다.</p>
          )}
          <ul style={{ listStyle: 'none', padding: 0, margin: 0 }}>
            {(reportsPage?.content ?? []).map((r) => (
              <li
                key={r.id}
                onClick={() => navigate(`/university-reports/${r.id}?lang=ko`)}
                style={{
                  padding: '14px 4px', borderBottom: `1px solid ${C.border}`, cursor: 'pointer',
                  transition: 'background 0.12s',
                }}
                onMouseEnter={(e) => { (e.currentTarget as HTMLLIElement).style.background = C.card }}
                onMouseLeave={(e) => { (e.currentTarget as HTMLLIElement).style.background = 'transparent' }}
              >
                <div style={{ fontWeight: 600, fontSize: 15, color: C.textPrimary, marginBottom: 4 }}>{r.title}</div>
                {r.summary && (
                  <div style={{ fontSize: 13, color: C.textSec, lineHeight: 1.6, marginBottom: 6,
                    display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                    {r.summary}
                  </div>
                )}
                <div style={{ display: 'flex', gap: 12, fontSize: 12, color: C.textDim }}>
                  <span>영상 {r.sourceVideoCount}개 분석</span>
                  {r.sourceWebCount > 0 && <span>웹 {r.sourceWebCount}개</span>}
                  <span style={{ marginLeft: 'auto' }}>{r.supportedLangs.map((l) => l.toUpperCase()).join(' · ')}</span>
                </div>
              </li>
            ))}
          </ul>
          {(reportsPage?.totalElements ?? 0) > 0 && (
            <div style={{ textAlign: 'center', marginTop: 16 }}>
              <Link to="/university-reports" style={{ color: C.activeText, fontSize: 13, fontWeight: 600 }}>
                AI 보고서 전체 보기 →
              </Link>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

// ── 게시글 테이블 (DC Inside 스타일) ─────────────────────────────────────────
interface PostTableProps {
  posts: import('../../types').Post[]
  isLoading: boolean
  showBoardName: boolean
  emptyText: string
  page: number
  hasNext: boolean
  totalElements: number
  onPrev: () => void
  onNext: () => void
}


const C2 = {
  border:      '#2d3748',
  borderLight: '#374151',
  textPrimary: '#f1f5f9',
  textSec:     '#cbd5e1',
  textMuted:   '#94a3b8',
  textDim:     '#64748b',
  card:        '#1e2836',
  activeText:  '#60a5fa',
}

function PostTable({ posts, isLoading, showBoardName, emptyText, page, hasNext, totalElements, onPrev, onNext }: PostTableProps) {
  if (isLoading) return <p style={{ color: C2.textMuted, padding: '24px 0' }}>불러오는 중...</p>
  if (posts.length === 0) return <p style={{ color: C2.textDim, padding: '40px 0', textAlign: 'center' }}>{emptyText}</p>

  return (
    <div>
      {/* 테이블 헤더 */}
      <div style={{
        display: 'grid',
        gridTemplateColumns: '60px 1fr 56px',
        gap: 4, padding: '8px 4px', borderBottom: `1px solid ${C2.border}`,
        fontSize: 12, color: C2.textDim, fontWeight: 600,
      }}>
        <span>번호</span>
        <span>{showBoardName ? '제목 (게시판)' : '제목'}</span>
        <span style={{ textAlign: 'center' }}>👍</span>
      </div>

      {posts.map((post, idx) => (
        <Link
          key={post.id}
          to={`/posts/${post.id}`}
          style={{ textDecoration: 'none', color: 'inherit' }}
        >
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: '60px 1fr 56px',
              gap: 4, padding: '10px 4px',
              borderBottom: `1px solid ${C2.border}`,
              alignItems: 'center',
              transition: 'background 0.12s',
              cursor: 'pointer',
            }}
            onMouseEnter={(e) => { (e.currentTarget as HTMLDivElement).style.background = C2.card }}
            onMouseLeave={(e) => { (e.currentTarget as HTMLDivElement).style.background = 'transparent' }}
          >
            <span style={{ fontSize: 12, color: C2.textDim }}>
              {totalElements > 0 ? totalElements - (page * 20) - idx : idx + 1}
            </span>
            <span>
              {post.deleted ? (
                <span style={{ fontSize: 14, color: C2.textDim }}>[삭제된 게시글]</span>
              ) : (
                <>
                  <span style={{ fontSize: 14, color: C2.textPrimary, fontWeight: 500 }}>{post.title}</span>
                  {showBoardName && post.boardName && (
                    <span style={{
                      marginLeft: 8, fontSize: 11, color: C2.activeText,
                      background: '#1e3a5f', padding: '2px 6px', borderRadius: 4,
                    }}>
                      {post.boardName}
                    </span>
                  )}
                </>
              )}
            </span>
            <span style={{ fontSize: 12, color: post.likeCount > 0 ? '#f59e0b' : C2.textDim, textAlign: 'center' }}>
              {post.likeCount > 0 ? post.likeCount : ''}
            </span>
          </div>
        </Link>
      ))}

      {totalElements > 20 && (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 16, marginTop: 20 }}>
          <button
            onClick={onPrev}
            disabled={page === 0}
            style={{
              padding: '7px 18px', borderRadius: 8, border: `1px solid ${C2.borderLight}`,
              background: page === 0 ? '#1a2234' : C2.card,
              color: page === 0 ? '#475569' : C2.textSec,
              cursor: page === 0 ? 'default' : 'pointer', fontSize: 13,
            }}
          >
            ← 이전
          </button>
          <span style={{ color: C2.textMuted, fontSize: 13 }}>
            {page + 1}페이지 ({totalElements}개)
          </span>
          <button
            onClick={onNext}
            disabled={!hasNext}
            style={{
              padding: '7px 18px', borderRadius: 8, border: `1px solid ${C2.borderLight}`,
              background: !hasNext ? '#1a2234' : C2.card,
              color: !hasNext ? '#475569' : C2.textSec,
              cursor: !hasNext ? 'default' : 'pointer', fontSize: 13,
            }}
          >
            다음 →
          </button>
        </div>
      )}
    </div>
  )
}
