'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'
import { useI18n } from '../../i18n/I18nProvider'
import { useMyOffice, usePartnerSettings, useUpdatePartnerSettings } from '../../hooks/useOffice'
import { PARTNER_CATEGORIES, type PartnerCategory } from '../../api/office'
import { Button, Card, EmptyState, PageHeader, QueryErrorNotice, SkeletonLines } from '../../components/ui'

/** 국제처 관리자 설정: 학생에게 보여 줄 제휴 분류. 담당자(STAFF)는 읽기만. */
export default function OfficeSettingsPage() {
  const { t } = useI18n()
  const myOffice = useMyOffice()
  const office = myOffice.data ?? null
  const officeId = office?.role ? office.officeId : undefined
  const isAdmin = office?.role === 'OFFICE_ADMIN'

  const settings = usePartnerSettings(officeId)
  const update = useUpdatePartnerSettings(officeId)
  const [enabled, setEnabled] = useState<PartnerCategory[]>([])

  useEffect(() => {
    if (settings.data) setEnabled(settings.data)
  }, [settings.data])

  if (myOffice.isLoading) return <SkeletonLines count={4} />
  if (myOffice.isError) return <QueryErrorNotice onRetry={() => myOffice.refetch()} />
  if (!office || !office.role) {
    return (
      <div style={{ maxWidth: 760, margin: '0 auto' }}>
        <PageHeader title={t('office.settings.title')} />
        <EmptyState title={t('office.staffOnly')} action={<Link href="/my-university">{t('nav.myUniversity')}</Link>} />
      </div>
    )
  }

  const toggle = (c: PartnerCategory) =>
    setEnabled((prev) => (prev.includes(c) ? prev.filter((x) => x !== c) : [...prev, c]))

  return (
    <div style={{ maxWidth: 760, margin: '0 auto', display: 'flex', flexDirection: 'column', gap: 16 }}>
      <PageHeader
        eyebrow={office.officeName}
        title={t('office.settings.title')}
        actions={<Link href="/office">{t('office.dashboard.link')}</Link>}
      />

      <Card>
        <h3 style={{ margin: '0 0 6px', fontSize: 15 }}>{t('office.settings.partners')}</h3>
        <p style={{ margin: '0 0 14px', fontSize: 12, color: 'var(--color-text-muted)' }}>{t('office.settings.partnersHelp')}</p>

        {settings.isLoading && <SkeletonLines count={3} />}
        {settings.isError && <QueryErrorNotice onRetry={() => settings.refetch()} />}
        {settings.data && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {PARTNER_CATEGORIES.map((c) => (
              <label key={c} style={{ display: 'flex', alignItems: 'center', gap: 10, fontSize: 14 }}>
                <input type="checkbox" checked={enabled.includes(c)} disabled={!isAdmin} onChange={() => toggle(c)} />
                {t(`partner.category.${c}`)}
              </label>
            ))}
            {isAdmin ? (
              <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginTop: 6 }}>
                <Button variant="primary" disabled={update.isPending} onClick={() => update.mutate(enabled)}>
                  {t('office.settings.save')}
                </Button>
                {update.isSuccess && <span style={{ fontSize: 12, color: 'var(--color-text-muted)' }}>{t('office.settings.saved')}</span>}
                {update.isError && <span style={{ fontSize: 12, color: 'var(--color-danger)' }}>{t('error.unexpected')}</span>}
              </div>
            ) : (
              <p style={{ margin: '6px 0 0', fontSize: 12, color: 'var(--color-text-muted)' }}>{t('office.settings.adminOnly')}</p>
            )}
          </div>
        )}
      </Card>
    </div>
  )
}
