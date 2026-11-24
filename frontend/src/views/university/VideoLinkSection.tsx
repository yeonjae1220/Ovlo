'use client'

import { useState } from 'react'
import { useUniversityVideoLinks } from '../../hooks/useUniversity'
import { useI18n } from '../../i18n/I18nProvider'
import { youtubeLink } from '../../utils/youtube'

const C = {
  card: 'var(--color-surface)',
  border: 'var(--color-border)',
  textPrimary: 'var(--color-text)',
  textMuted: 'var(--color-text-muted)',
  textDim: 'var(--color-text-dim)',
  activeText: 'var(--color-info)',
}

/**
 * 게시일 표시. locale 은 주소창의 lang 값에서 오므로 잘못된 값(en_US 등)이 들어올 수 있다 —
 * 그때는 브라우저 기본 로캘로 물러서고, 날짜를 읽을 수 없으면 아무것도 표시하지 않는다.
 * 게시 시각은 UTC 날짜로 고정해 보는 곳의 시간대에 따라 하루씩 달라지지 않게 한다.
 */
export function formatPublishedDate(publishedAt: string | null | undefined, locale: string): string | null {
  if (!publishedAt) return null
  const date = new Date(publishedAt)
  if (Number.isNaN(date.getTime())) return null
  try {
    return date.toLocaleDateString(locale, { timeZone: 'UTC' })
  } catch {
    return date.toLocaleDateString(undefined, { timeZone: 'UTC' })
  }
}

export const VIDEO_LINKS_FETCH_SIZE = 12
export const VIDEO_LINKS_COLLAPSED = 4

/**
 * 대학에 연결된 유튜브 영상 링크. 영상이 없거나 조회에 실패하면 아무것도 그리지 않는다 —
 * 리포트 본문이 주 내용이라 보조 구역의 오류로 화면을 가리지 않는다.
 */
export default function VideoLinkSection({ globalUnivId, locale }: { globalUnivId?: number | null; locale: string }) {
  const { t } = useI18n()
  const [expanded, setExpanded] = useState(false)
  const { data } = useUniversityVideoLinks(globalUnivId, VIDEO_LINKS_FETCH_SIZE)

  const links = (data?.content ?? [])
    .map((v) => ({ ...v, link: youtubeLink(v.youtubeUrl) }))
    .filter((v) => v.link !== null)
  if (links.length === 0) return null
  const shown = expanded ? links : links.slice(0, VIDEO_LINKS_COLLAPSED)

  return (
    <section
      aria-labelledby="video-links-title"
      style={{ background: C.card, borderRadius: 12, padding: '20px 24px', marginBottom: 24, border: `1px solid ${C.border}` }}
    >
      <h2 id="video-links-title" style={{ margin: 0, fontSize: 16, fontWeight: 700, color: C.textPrimary }}>
        {t('report.videos.title')}
      </h2>
      <p style={{ margin: '6px 0 14px', fontSize: 12, color: C.textDim, lineHeight: 1.6 }}>{t('report.videos.note')}</p>
      <ul id="video-links-list" style={{ listStyle: 'none', margin: 0, padding: 0, display: 'grid', gap: 12, gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))' }}>
        {shown.map((v) => (
          <li key={v.id}>
            <a
              href={v.link!.href}
              target="_blank"
              rel="noopener noreferrer"
              style={{ display: 'block', textDecoration: 'none', color: 'inherit' }}
            >
              <img
                src={v.link!.thumbnail}
                alt=""
                loading="lazy"
                width={320}
                height={180}
                style={{ width: '100%', height: 'auto', aspectRatio: '16 / 9', objectFit: 'cover', borderRadius: 8, border: `1px solid ${C.border}`, display: 'block' }}
              />
              <span style={{ display: 'block', marginTop: 8, fontSize: 14, fontWeight: 600, color: C.activeText, lineHeight: 1.4, overflowWrap: 'anywhere' }}>
                {v.title || 'YouTube'}
              </span>
              <span style={{ display: 'block', marginTop: 4, fontSize: 12, color: C.textMuted }}>
                {[v.channel, formatPublishedDate(v.publishedAt, locale)].filter(Boolean).join(' · ')}
              </span>
            </a>
          </li>
        ))}
      </ul>
      {links.length > VIDEO_LINKS_COLLAPSED && (
        <button
          type="button"
          onClick={() => setExpanded((x) => !x)}
          aria-expanded={expanded}
          aria-controls="video-links-list"
          style={{ marginTop: 14, background: 'none', border: `1px solid ${C.border}`, borderRadius: 8, padding: '8px 14px', fontSize: 13, color: C.textPrimary, cursor: 'pointer' }}
        >
          {expanded ? t('report.videos.less') : t('report.videos.more')}
        </button>
      )}
    </section>
  )
}
