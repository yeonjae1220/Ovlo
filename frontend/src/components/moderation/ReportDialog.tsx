'use client'

import { useId, useState } from 'react'
import {
  REPORT_DETAIL_MAX_LENGTH,
  REPORT_REASONS,
  reportErrorKey,
  type ReportReason,
  type ReportTargetType,
} from '../../api/moderation'
import { useReportContent } from '../../hooks/useModeration'
import { useI18n } from '../../i18n/I18nProvider'
import { Button, TextAreaField } from '../ui'
import { ModerationSheet } from './ModerationSheet'

export interface ReportTarget {
  type: ReportTargetType
  id: number
}

const TITLE_KEY: Record<ReportTargetType, string> = {
  POST: 'moderation.report.title.POST',
  COMMENT: 'moderation.report.title.COMMENT',
  CHAT_MESSAGE: 'moderation.report.title.CHAT_MESSAGE',
  MEMBER: 'moderation.report.title.MEMBER',
}

/**
 * 게시글·댓글·채팅 메시지·회원 신고. 사유는 필수이고 '기타'일 때만 상세 설명이 필수다.
 * 같은 대상을 다시 신고해도 서버는 204 를 돌려주므로 성공 화면은 동일하다.
 */
export function ReportDialog({ target, onClose }: { target: ReportTarget; onClose: () => void }) {
  const { t } = useI18n()
  const titleId = useId()
  const report = useReportContent()
  const [reason, setReason] = useState<ReportReason | null>(null)
  const [detail, setDetail] = useState('')
  const [showValidation, setShowValidation] = useState(false)

  const trimmedDetail = detail.trim()
  const detailRequired = reason === 'OTHER'
  const missingReason = reason === null
  const missingDetail = detailRequired && trimmedDetail.length === 0
  const busy = report.isPending

  const submit = () => {
    if (busy) return
    if (missingReason || missingDetail) {
      setShowValidation(true)
      return
    }
    report.mutate({
      targetType: target.type,
      targetId: target.id,
      reason,
      detail: trimmedDetail || undefined,
    })
  }

  if (report.isSuccess) {
    return (
      <ModerationSheet titleId={titleId} busy={false} onClose={onClose}>
        <h2 id={titleId} style={{ margin: '0 0 10px', fontSize: 18 }}>{t('moderation.report.done.title')}</h2>
        <p role="status" style={{ margin: '0 0 16px', lineHeight: 1.6, color: 'var(--color-text-secondary)' }}>
          {t('moderation.report.done.body')}
        </p>
        <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
          <Button variant="primary" onClick={onClose}>{t('moderation.close')}</Button>
        </div>
      </ModerationSheet>
    )
  }

  return (
    <ModerationSheet titleId={titleId} busy={busy} onClose={onClose}>
      <h2 id={titleId} style={{ margin: '0 0 6px', fontSize: 18 }}>{t(TITLE_KEY[target.type])}</h2>
      <p style={{ margin: '0 0 14px', fontSize: 13, lineHeight: 1.6, color: 'var(--color-text-secondary)' }}>
        {t('moderation.report.intro')}
      </p>

      <fieldset style={{ border: 'none', margin: 0, padding: 0 }} aria-describedby={showValidation && missingReason ? `${titleId}-reason-error` : undefined}>
        <legend style={{ fontWeight: 700, fontSize: 14, marginBottom: 8 }}>{t('moderation.report.reasonLabel')}</legend>
        <div style={{ display: 'grid', gap: 4 }}>
          {REPORT_REASONS.map((value) => (
            <label
              key={value}
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 10,
                padding: '9px 10px',
                minHeight: 40,
                borderRadius: 10,
                cursor: busy ? 'default' : 'pointer',
                border: `1px solid ${reason === value ? 'var(--color-accent)' : 'var(--color-border)'}`,
                background: reason === value ? 'var(--color-accent-soft)' : 'transparent',
              }}
            >
              <input
                type="radio"
                name={`${titleId}-reason`}
                value={value}
                checked={reason === value}
                disabled={busy}
                onChange={() => setReason(value)}
              />
              <span style={{ fontSize: 14 }}>{t(`moderation.reason.${value}`)}</span>
            </label>
          ))}
        </div>
        {showValidation && missingReason && (
          <p id={`${titleId}-reason-error`} role="alert" style={{ margin: '6px 0 0', color: 'var(--color-danger)', fontSize: 13 }}>
            {t('moderation.report.reasonRequired')}
          </p>
        )}
      </fieldset>

      <label style={{ display: 'grid', gap: 6, marginTop: 14 }}>
        <span style={{ fontWeight: 700, fontSize: 14 }}>
          {detailRequired ? t('moderation.report.detailLabelRequired') : t('moderation.report.detailLabel')}
        </span>
        <TextAreaField
          value={detail}
          onChange={(e) => setDetail(e.target.value)}
          maxLength={REPORT_DETAIL_MAX_LENGTH}
          rows={3}
          disabled={busy}
          aria-invalid={showValidation && missingDetail}
          placeholder={t('moderation.report.detailPlaceholder')}
          style={{ fontSize: 16 }}
        />
        <span style={{ fontSize: 12, color: 'var(--color-text-dim)', textAlign: 'right' }}>
          {detail.length}/{REPORT_DETAIL_MAX_LENGTH}
        </span>
      </label>
      {showValidation && missingDetail && (
        <p role="alert" style={{ margin: '0 0 6px', color: 'var(--color-danger)', fontSize: 13 }}>
          {t('moderation.report.detailRequired')}
        </p>
      )}

      {report.isError && (
        <p role="alert" style={{ margin: '6px 0 0', color: 'var(--color-danger)', fontSize: 13 }}>
          {t(reportErrorKey(report.error))}
        </p>
      )}

      <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 14 }}>
        <Button variant="ghost" onClick={onClose} disabled={busy}>{t('moderation.cancel')}</Button>
        <Button variant="danger" onClick={submit} disabled={busy}>
          {busy ? t('moderation.report.submitting') : t('moderation.report.submit')}
        </Button>
      </div>
    </ModerationSheet>
  )
}
