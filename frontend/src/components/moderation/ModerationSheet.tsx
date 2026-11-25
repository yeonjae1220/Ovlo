'use client'

import { useEffect, useRef, type ReactNode } from 'react'
import { useBreakpoint } from '../../hooks/useBreakpoint'

const FOCUSABLE = 'button:not(:disabled), a[href], input:not(:disabled), textarea:not(:disabled), [tabindex="0"]'

/**
 * 신고·차단 확인에 쓰는 모달. 휴대폰 폭(네이티브 웹뷰 포함)에서는 하단 시트로 열고
 * 홈 인디케이터 영역(safe-area-inset-bottom)만큼 띄운다. 포커스 트랩·Esc 닫기는 ConsentDialog 와 같은 방식.
 */
export function ModerationSheet({
  titleId,
  busy,
  onClose,
  children,
}: {
  titleId: string
  busy: boolean
  onClose: () => void
  children: ReactNode
}) {
  const dialog = useRef<HTMLDivElement>(null)
  const { isMobile } = useBreakpoint()

  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null
    dialog.current?.focus()
    return () => previous?.focus()
  }, [])

  return (
    <div
      onClick={(e) => { if (e.target === e.currentTarget && !busy) onClose() }}
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(0,0,0,0.45)',
        display: 'flex',
        alignItems: isMobile ? 'flex-end' : 'center',
        justifyContent: 'center',
        padding: isMobile ? 0 : 16,
        zIndex: 10000,
      }}
    >
      <div
        ref={dialog}
        tabIndex={-1}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        onKeyDown={(e) => {
          if (e.key === 'Escape' && !busy) { e.preventDefault(); onClose() }
          if (e.key !== 'Tab') return
          const elements = Array.from(dialog.current?.querySelectorAll<HTMLElement>(FOCUSABLE) ?? [])
          const first = elements[0], last = elements.at(-1)
          if (!first) { e.preventDefault(); return }
          if (e.shiftKey && (document.activeElement === first || document.activeElement === dialog.current)) { e.preventDefault(); last?.focus() }
          else if (!e.shiftKey && (document.activeElement === last || document.activeElement === dialog.current)) { e.preventDefault(); first.focus() }
        }}
        style={{
          width: '100%',
          maxWidth: isMobile ? '100%' : 480,
          maxHeight: isMobile ? '88dvh' : '90dvh',
          overflowY: 'auto',
          background: 'var(--color-surface)',
          color: 'var(--color-text)',
          border: '1px solid var(--color-border)',
          borderRadius: isMobile ? '18px 18px 0 0' : 14,
          padding: isMobile ? '18px 16px max(18px, env(safe-area-inset-bottom))' : 20,
          boxShadow: 'var(--shadow-soft)',
          outline: 'none',
        }}
      >
        {children}
      </div>
    </div>
  )
}
