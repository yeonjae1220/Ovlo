'use client'

import { useEffect, useMemo, useState } from 'react'
import { Badge, Button, FieldGroup, SearchBox, TextField } from '../../components/ui'
import { useI18n } from '../../i18n/I18nProvider'
import { useGlobalUniversitySearch } from '../../hooks/useUniversity'
import {
  extractVerificationErrorCode,
  useConfirmEmailVerification,
  useMyVerification,
  useRequestEmailVerification,
} from '../../hooks/useVerification'
import { TrustBadge } from './TrustBadge'

const C = {
  border: 'var(--color-border)',
  surfaceSoft: 'var(--color-surface-soft)',
  textPrimary: 'var(--color-text)',
  textSec: 'var(--color-text-secondary)',
  textMuted: 'var(--color-text-muted)',
  textDim: 'var(--color-text-dim)',
  accent: 'var(--color-accent)',
  accentSoft: 'var(--color-accent-soft)',
  danger: 'var(--color-danger)',
  dangerSoft: 'var(--color-danger-soft)',
}

type SelectedUniversity = { id: number; name: string; city?: string; countryCode?: string }

/** 발송 후 재발송 허용까지의 클라이언트 쿨다운(초) — 서버 rate limit 보조. */
const RESEND_COOLDOWN_SECONDS = 30

export function VerificationSection() {
  const { t } = useI18n()
  const { data: status, isLoading } = useMyVerification()

  const [open, setOpen] = useState(false)

  if (isLoading) {
    return <p style={{ color: C.textMuted, fontSize: 14 }}>{t('verification.loading')}</p>
  }

  return (
    <div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
        <span style={{ fontSize: 14, color: C.textSec }}>{t('verification.currentLevel')}</span>
        <TrustBadge level={status?.trustLevel ?? 'UNVERIFIED'} showUnverified />
      </div>

      {status && status.verifiedUniversities.length > 0 && (
        <ul style={{ listStyle: 'none', padding: 0, margin: '12px 0 0', display: 'flex', flexDirection: 'column', gap: 6 }}>
          {status.verifiedUniversities.map((u) => (
            <li
              key={u.universityId}
              style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13, color: C.textSec }}
            >
              <Badge tone="success">✓</Badge>
              <span>{u.verifiedEmail}</span>
            </li>
          ))}
        </ul>
      )}

      <div style={{ marginTop: 16 }}>
        {open ? (
          <VerificationFlow onDone={() => setOpen(false)} />
        ) : (
          <Button variant="primary" onClick={() => setOpen(true)}>
            {status && status.verifiedUniversities.length > 0
              ? t('verification.addAnother')
              : t('verification.start')}
          </Button>
        )}
      </div>
    </div>
  )
}

function VerificationFlow({ onDone }: { onDone: () => void }) {
  const { t } = useI18n()
  const requestMutation = useRequestEmailVerification()
  const confirmMutation = useConfirmEmailVerification()

  // step 1 — 대학 선택 + 학교 이메일
  const [keyword, setKeyword] = useState('')
  const [debounced, setDebounced] = useState('')
  const [selected, setSelected] = useState<SelectedUniversity | null>(null)
  const [schoolEmail, setSchoolEmail] = useState('')

  // step 2 — 코드 확인
  const [sent, setSent] = useState<{ maskedEmail: string; expiresAt: number } | null>(null)
  const [code, setCode] = useState('')
  const [now, setNow] = useState(() => Date.now())
  const [cooldownUntil, setCooldownUntil] = useState(0)

  const { data: results, isFetching } = useGlobalUniversitySearch(debounced)

  // 검색 디바운스
  useEffect(() => {
    const h = setTimeout(() => setDebounced(keyword.trim()), 300)
    return () => clearTimeout(h)
  }, [keyword])

  // 만료/쿨다운 카운트다운 (1초 틱) — step 2일 때만
  useEffect(() => {
    if (!sent) return
    const h = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(h)
  }, [sent])

  const remainingSeconds = sent ? Math.max(0, Math.ceil((sent.expiresAt - now) / 1000)) : 0
  const cooldownSeconds = Math.max(0, Math.ceil((cooldownUntil - now) / 1000))
  const expired = sent !== null && remainingSeconds === 0

  const requestErrorCode = extractVerificationErrorCode(requestMutation.error)
  const confirmErrorCode = extractVerificationErrorCode(confirmMutation.error)

  const emailValid = useMemo(() => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(schoolEmail), [schoolEmail])
  const canRequest = selected !== null && emailValid && !requestMutation.isPending
  const canConfirm = /^\d{6}$/.test(code) && !confirmMutation.isPending && !expired

  const sendCode = async () => {
    if (!selected) return
    const result = await requestMutation.mutateAsync({ universityId: selected.id, schoolEmail })
    setSent({ maskedEmail: result.maskedEmail, expiresAt: Date.now() + result.expiresInSeconds * 1000 })
    setCode('')
    setCooldownUntil(Date.now() + RESEND_COOLDOWN_SECONDS * 1000)
    setNow(Date.now())
  }

  const confirm = async () => {
    await confirmMutation.mutateAsync(code)
    onDone()
  }

  // ── step 2: 코드 입력 ──
  if (sent) {
    return (
      <div style={panelStyle}>
        <p style={{ margin: 0, fontSize: 14, color: C.textSec }}>
          {t('verification.codeSentTo')} <strong style={{ color: C.textPrimary }}>{sent.maskedEmail}</strong>
        </p>
        <p style={{ margin: '4px 0 0', fontSize: 12, color: expired ? C.danger : C.textMuted }}>
          {expired
            ? t('verification.codeExpiredHint')
            : t('verification.expiresIn', { time: formatMmss(remainingSeconds) })}
        </p>

        <FieldGroup label={t('verification.codeLabel')} hint={t('verification.codeHint')}>
          <TextField
            value={code}
            onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
            inputMode="numeric"
            autoComplete="one-time-code"
            placeholder="000000"
            style={{ letterSpacing: '0.4em', fontSize: 18, textAlign: 'center' }}
          />
        </FieldGroup>

        {confirmErrorCode && <ErrorNote text={t(`verification.error.${confirmErrorCode}` as never)} />}

        <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap', marginTop: 4 }}>
          <Button variant="primary" disabled={!canConfirm} onClick={confirm}>
            {confirmMutation.isPending ? t('verification.confirming') : t('verification.confirm')}
          </Button>
          <Button
            variant="ghost"
            disabled={cooldownSeconds > 0 || requestMutation.isPending}
            onClick={sendCode}
          >
            {cooldownSeconds > 0
              ? t('verification.resendIn', { seconds: String(cooldownSeconds) })
              : t('verification.resend')}
          </Button>
          <Button variant="ghost" onClick={onDone}>{t('common.cancel')}</Button>
        </div>
      </div>
    )
  }

  // ── step 1: 대학 + 이메일 ──
  return (
    <div style={panelStyle}>
      <FieldGroup label={t('verification.universityLabel')} hint={t('verification.universityHint')}>
        {selected ? (
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8, padding: '8px 12px', border: `1px solid ${C.border}`, borderRadius: 8, background: C.surfaceSoft }}>
            <span style={{ fontSize: 14, color: C.textPrimary }}>
              {selected.name}
              {selected.city ? <span style={{ color: C.textMuted }}> · {selected.city}</span> : null}
            </span>
            <Button variant="ghost" onClick={() => setSelected(null)}>{t('verification.changeUniversity')}</Button>
          </div>
        ) : (
          <>
            <SearchBox
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder={t('verification.universitySearchPlaceholder')}
            />
            {debounced.length >= 1 && (
              <div style={{ marginTop: 6, border: `1px solid ${C.border}`, borderRadius: 8, maxHeight: 220, overflowY: 'auto' }}>
                {isFetching && <p style={{ margin: 0, padding: '10px 12px', fontSize: 13, color: C.textMuted }}>{t('verification.searching')}</p>}
                {!isFetching && (results?.length ?? 0) === 0 && (
                  <p style={{ margin: 0, padding: '10px 12px', fontSize: 13, color: C.textDim }}>{t('verification.noUniversity')}</p>
                )}
                {(results ?? []).map((u) => (
                  <button
                    key={u.id}
                    onClick={() => { setSelected(u); setKeyword(''); setDebounced('') }}
                    style={{ display: 'block', width: '100%', textAlign: 'left', padding: '10px 12px', border: 'none', borderBottom: `1px solid ${C.border}`, background: 'transparent', cursor: 'pointer', fontSize: 14, color: C.textPrimary }}
                  >
                    {u.name}
                    {u.city ? <span style={{ color: C.textMuted, fontSize: 12 }}> · {u.city}</span> : null}
                  </button>
                ))}
              </div>
            )}
          </>
        )}
      </FieldGroup>

      <FieldGroup label={t('verification.emailLabel')} hint={t('verification.emailHint')}>
        <TextField
          type="email"
          value={schoolEmail}
          onChange={(e) => setSchoolEmail(e.target.value)}
          placeholder="you@univ.edu"
          autoComplete="email"
        />
      </FieldGroup>

      {requestErrorCode && <ErrorNote text={t(`verification.error.${requestErrorCode}` as never)} />}

      <div style={{ display: 'flex', gap: 8, marginTop: 4 }}>
        <Button variant="primary" disabled={!canRequest} onClick={sendCode}>
          {requestMutation.isPending ? t('verification.sending') : t('verification.sendCode')}
        </Button>
        <Button variant="ghost" onClick={onDone}>{t('common.cancel')}</Button>
      </div>
    </div>
  )
}

const panelStyle: React.CSSProperties = {
  display: 'flex',
  flexDirection: 'column',
  gap: 14,
  padding: 16,
  border: `1px solid ${C.border}`,
  borderRadius: 12,
  background: C.surfaceSoft,
}

function ErrorNote({ text }: { text: string }) {
  return (
    <p style={{ margin: 0, padding: '8px 12px', fontSize: 13, color: C.danger, background: C.dangerSoft, borderRadius: 8 }}>
      {text}
    </p>
  )
}

function formatMmss(totalSeconds: number): string {
  const m = Math.floor(totalSeconds / 60)
  const s = totalSeconds % 60
  return `${m}:${String(s).padStart(2, '0')}`
}
