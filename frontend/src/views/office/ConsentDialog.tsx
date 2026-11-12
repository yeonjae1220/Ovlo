'use client'

import { useEffect, useRef, type ReactNode } from 'react'
import { useI18n } from '../../i18n/I18nProvider'
import { CONSULTATION_CONSENT } from '../legal/legalContent'
import { Button, Card } from '../../components/ui'

/**
 * 상담 이관 동의 화면. 문구는 legalContent.ts 의 CONSULTATION_CONSENT(version 포함)이며 서버가 같은 version 의 정본을 기록한다.
 * 동의 없이는 전달되지 않는다.
 */
export default function ConsentDialog({
  pending,
  preview,
  error,
  onAgree,
  onDecline,
}: {
  pending: boolean
  preview?: ReactNode
  error: string | null
  onAgree: (consentVersion: string) => void
  onDecline: () => void
}) {
  const dialog = useRef<HTMLDivElement>(null)
  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null
    dialog.current?.focus()
    return () => previous?.focus()
  }, [])
  const { language } = useI18n()
  const text = language === 'ko' ? CONSULTATION_CONSENT.ko : CONSULTATION_CONSENT.en

  return (
    <div
      ref={dialog}
      tabIndex={-1}
      onKeyDown={e => {
        if (e.key === 'Escape' && !pending) { e.preventDefault(); onDecline() }
        if (e.key !== 'Tab') return
        const elements = Array.from(dialog.current?.querySelectorAll<HTMLElement>('button:not(:disabled), a[href], [tabindex="0"]') ?? [])
        const first = elements[0], last = elements.at(-1)
        if (!first) { e.preventDefault(); return }
        if (e.shiftKey && (document.activeElement === first || document.activeElement === dialog.current)) { e.preventDefault(); last?.focus() }
        else if (!e.shiftKey && (document.activeElement === last || document.activeElement === dialog.current)) { e.preventDefault(); first.focus() }
      }}
      role="dialog"
      aria-modal="true"
      aria-labelledby="consent-title"
      style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.45)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 16, zIndex: 10000 }}
    >
      <Card style={{ padding: 18, background: 'var(--color-surface)', maxWidth: 520, width: '100%', maxHeight: '90dvh', overflowY: 'auto' }}>
        <h2 id="consent-title" style={{ margin: '0 0 10px', fontSize: 18 }}>{text.title}</h2>
        {text.body.map((p, i) => (
          <p key={i} style={{ margin: '0 0 8px', lineHeight: 1.7 }}>{p}</p>
        ))}
        <ul style={{ margin: '0 0 10px', paddingLeft: 20, lineHeight: 1.7 }}>
          {text.shared.map((s, i) => (
            <li key={i}>{s}</li>
          ))}
        </ul>
        {text.notes.map((n, i) => (
          <p key={i} style={{ margin: '0 0 6px', fontSize: 13, color: 'var(--color-text-secondary)', lineHeight: 1.6 }}>{n}</p>
        ))}
        {preview && <div style={{ borderTop: '1px solid var(--color-border)', paddingTop: 12 }}>{preview}</div>}
        {error && (
          <p role="alert" style={{ margin: '8px 0 0', color: 'var(--color-danger, #c0392b)', fontSize: 13 }}>{error}</p>
        )}
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 14, position: 'sticky', bottom: -18, background: 'var(--color-surface)', padding: '12px 0' }}>
          <Button variant="ghost" onClick={onDecline} disabled={pending}>{text.decline}</Button>
          <Button variant="primary" onClick={() => onAgree(CONSULTATION_CONSENT.version)} disabled={pending}>{text.agree}</Button>
        </div>
      </Card>
    </div>
  )
}
