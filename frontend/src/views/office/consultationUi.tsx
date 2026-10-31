'use client'

import { Badge, Card } from '../../components/ui'
import type { ConsultationStatus, TranscriptMessage } from '../../api/office'
import { useI18n } from '../../i18n/I18nProvider'

export function ConsultationStatusBadge({ status }: { status: ConsultationStatus }) {
  const { t } = useI18n()
  const tone = status === 'OPEN' ? 'warning' : status === 'IN_PROGRESS' ? 'info' : status === 'RESOLVED' ? 'success' : 'neutral'
  return <Badge tone={tone}>{t(`consult.status.${status}`)}</Badge>
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  return new Intl.DateTimeFormat(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }).format(d)
}

/** 대화 목록. 상대방 메시지는 번역을 앞에, 원문을 접은 글씨로 함께 보여 준다. */
export function Transcript({ messages, viewerId }: { messages: TranscriptMessage[]; viewerId: number | null }) {
  const { t } = useI18n()
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      {messages.map((m) => {
        const mine = viewerId !== null && m.senderId === viewerId
        const who = mine ? t('consult.you') : m.fromStaff ? t('consult.staff') : t('consult.student')
        return (
          <Card key={m.messageId} style={{ alignSelf: mine ? 'flex-end' : 'flex-start', maxWidth: '85%' }}>
            <div style={{ fontSize: 12, color: 'var(--color-text-muted)', marginBottom: 4 }}>
              {who} · {formatDateTime(m.sentAt)}
            </div>
            {m.translatedText ? (
              <>
                <p style={{ margin: 0, whiteSpace: 'pre-wrap', lineHeight: 1.6 }}>{m.translatedText}</p>
                <details style={{ marginTop: 6, fontSize: 13, color: 'var(--color-text-secondary)' }}>
                  <summary>{t('consult.original')}</summary>
                  <p style={{ margin: '4px 0 0', whiteSpace: 'pre-wrap' }}>{m.content}</p>
                </details>
              </>
            ) : (
              <>
                <p style={{ margin: 0, whiteSpace: 'pre-wrap', lineHeight: 1.6 }}>{m.content}</p>
                {m.translationFailed && (
                  <p style={{ margin: '6px 0 0', fontSize: 12, color: 'var(--color-warm)' }}>{t('consult.translationFailed')}</p>
                )}
              </>
            )}
          </Card>
        )
      })}
    </div>
  )
}
