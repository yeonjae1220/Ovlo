'use client'

import Link from 'next/link'
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
