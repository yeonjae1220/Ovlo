'use client'

import Link from 'next/link'
import { useI18n } from '../../i18n/I18nProvider'
import type {
  ButtonHTMLAttributes,
  HTMLAttributes,
  InputHTMLAttributes,
  ReactNode,
  SelectHTMLAttributes,
  TextareaHTMLAttributes,
} from 'react'

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger'

function cx(...values: Array<string | false | null | undefined>) {
  return values.filter(Boolean).join(' ')
}

export function Button({
  className,
  variant = 'secondary',
  icon,
  children,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant; icon?: ReactNode }) {
  return (
    <button className={cx('ui-button', `ui-button--${variant}`, className)} {...props}>
      {icon && <span className="ui-button__icon" aria-hidden="true">{icon}</span>}
      <span>{children}</span>
    </button>
  )
}

export function LinkButton({
  className,
  variant = 'secondary',
  icon,
  href,
  children,
}: {
  className?: string
  variant?: Variant
  icon?: ReactNode
  href: string
  children: ReactNode
}) {
  return (
    <Link href={href} className={cx('ui-button', `ui-button--${variant}`, className)}>
      {icon && <span className="ui-button__icon" aria-hidden="true">{icon}</span>}
      <span>{children}</span>
    </Link>
  )
}

export function IconButton({
  className,
  label,
  children,
  variant = 'ghost',
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { label: string; variant?: Variant }) {
  return (
    <button
      className={cx('ui-icon-button', `ui-button--${variant}`, className)}
      aria-label={label}
      title={label}
      {...props}
    >
      {children}
    </button>
  )
}

export function Card({
  className,
  interactive,
  ...props
}: HTMLAttributes<HTMLDivElement> & { interactive?: boolean }) {
  return <div className={cx('ui-card', interactive && 'ui-card--interactive', className)} {...props} />
}

export function Badge({
  className,
  tone = 'neutral',
  ...props
}: HTMLAttributes<HTMLSpanElement> & { tone?: 'neutral' | 'accent' | 'info' | 'success' | 'warning' | 'danger' }) {
  return <span className={cx('ui-badge', `ui-badge--${tone}`, className)} {...props} />
}

export function Avatar({ label, imageUrl, size = 'md' }: { label: string; imageUrl?: string | null; size?: 'sm' | 'md' | 'lg' }) {
  const initial = label.trim()[0]?.toUpperCase() ?? '?'
  if (imageUrl) {
    return <img className={cx('ui-avatar', `ui-avatar--${size}`)} src={imageUrl} alt={label} />
  }
  return (
    <span className={cx('ui-avatar', `ui-avatar--${size}`)} aria-label={label}>
      {initial}
    </span>
  )
}

export function PageHeader({
  eyebrow,
  title,
  description,
  actions,
}: {
  eyebrow?: ReactNode
  title: ReactNode
  description?: ReactNode
  actions?: ReactNode
}) {
  return (
    <div className="ui-page-header">
      <div>
        {eyebrow && <div className="ui-page-header__eyebrow">{eyebrow}</div>}
        <h1 className="ui-page-header__title">{title}</h1>
        {description && <p className="ui-page-header__description">{description}</p>}
      </div>
      {actions && <div className="ui-page-header__actions">{actions}</div>}
    </div>
  )
}

export function TextField({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={cx('ui-field', className)} {...props} />
}

export function SearchBox({ className, ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return (
    <div className={cx('ui-search', className)}>
      <span aria-hidden="true" className="ui-search__icon">⌕</span>
      <input className="ui-field ui-search__input" {...props} />
    </div>
  )
}

export function SelectField({ className, ...props }: SelectHTMLAttributes<HTMLSelectElement>) {
  return <select className={cx('ui-field ui-select', className)} {...props} />
}

export function TextAreaField({ className, ...props }: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea className={cx('ui-field ui-textarea', className)} {...props} />
}

export function FieldGroup({ label, hint, children }: { label?: ReactNode; hint?: ReactNode; children: ReactNode }) {
  return (
    <label className="ui-field-group">
      {label && <span className="ui-field-group__label">{label}</span>}
      {children}
      {hint && <span className="ui-field-group__hint">{hint}</span>}
    </label>
  )
}

export function Tabs<T extends string>({
  items,
  value,
  onChange,
}: {
  items: Array<{ value: T; label: ReactNode; icon?: ReactNode }>
  value: T
  onChange: (value: T) => void
}) {
  return (
    <div className="ui-tabs" role="tablist">
      {items.map((item) => (
        <button
          key={item.value}
          type="button"
          role="tab"
          aria-selected={value === item.value}
          className={cx('ui-tab', value === item.value && 'ui-tab--active')}
          onClick={() => onChange(item.value)}
        >
          {item.icon && <span aria-hidden="true">{item.icon}</span>}
          <span>{item.label}</span>
        </button>
      ))}
    </div>
  )
}

export function EmptyState({
  icon,
  title,
  description,
  action,
}: {
  icon?: ReactNode
  title: ReactNode
  description?: ReactNode
  action?: ReactNode
}) {
  return (
    <div className="ui-empty">
      {icon && <div className="ui-empty__icon" aria-hidden="true">{icon}</div>}
      <div className="ui-empty__title">{title}</div>
      {description && <p className="ui-empty__description">{description}</p>}
      {action && <div className="ui-empty__action">{action}</div>}
    </div>
  )
}

export function SkeletonLines({ count = 3 }: { count?: number }) {
  return (
    <div className="ui-skeleton" aria-hidden="true">
      {Array.from({ length: count }).map((_, index) => (
        <span key={index} className="ui-skeleton__line" />
      ))}
    </div>
  )
}

/**
 * 쿼리 실패를 화면에 드러내는 공용 알림.
 *
 * 🔴 이 앱은 지금까지 쿼리 실패를 화면에 표시하는 수단이 아예 없었다 — `isError` 는 프론트 전체에서
 *    4번 나오는데 전부 mutation 이고, 쿼리 결과를 구조분해하는 37곳 중 error 를 함께 꺼내는 곳이
 *    0곳이었다. react-query 는 실패 시 data 가 undefined 라 `data?.content ?? []` 가 그대로
 *    **빈 목록**을 그리므로, 사용자에게 '조회 실패'와 '데이터 없음'이 똑같이 보였다
 *    (GLOBAL-PIT-108). isLoading 은 43곳에서 쓰여 loading/empty 는 이미 갈려 있었고 error 만 빠져 있었다.
 *
 * 사용 규칙:
 *   1. 실패 시에는 목록·빈 상태·파생값(합계·차트) 중 **아무것도 그리지 않는다** — 전부 거짓 정보가 된다.
 *   2. 실패 판정은 react-query 의 `isError` 로 한다(error 객체의 truthy 여부로 하지 않는다).
 *   3. `refetch` 를 넘겨 사용자가 직접 재시도할 수 있게 한다.
 */
export function QueryErrorNotice({
  onRetry,
  compact = false,
}: {
  onRetry?: () => void
  compact?: boolean
}) {
  const { t } = useI18n()
  return (
    <div
      role="alert"
      style={{
        display: 'flex',
        flexDirection: compact ? 'row' : 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 12,
        padding: compact ? '12px 16px' : '32px 20px',
        border: '1px solid var(--color-border)',
        borderRadius: 12,
        background: 'var(--color-surface)',
        textAlign: 'center',
      }}
    >
      <span style={{ color: 'var(--color-text-secondary)', fontSize: 14 }}>{t('error.unexpected')}</span>
      {onRetry && (
        <Button variant="secondary" onClick={onRetry}>
          {t('common.retry')}
        </Button>
      )}
    </div>
  )
}
