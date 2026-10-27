'use client'

import Link from 'next/link'

export function BrandMark({ size = 32 }: { size?: number }) {
  return (
    <svg
      aria-hidden="true"
      width={size}
      height={size}
      viewBox="0 0 40 40"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      style={{ display: 'block', flexShrink: 0 }}
    >
      <rect width="40" height="40" rx="12" fill="var(--color-accent-strong)" />
      <circle cx="20" cy="20" r="10.25" stroke="var(--color-on-accent)" strokeWidth="3.5" />
      <circle cx="8.5" cy="14" r="2.25" fill="var(--color-warm)" stroke="var(--color-on-accent)" strokeWidth="1.25" />
      <circle cx="31.5" cy="26" r="2.25" fill="var(--color-warning)" stroke="var(--color-on-accent)" strokeWidth="1.25" />
      <path d="M10 13.25C14 7.5 26 7.5 30 13.25" stroke="var(--color-on-accent)" strokeWidth="1.5" strokeLinecap="round" opacity=".72" />
    </svg>
  )
}

export function Brand({ compact = false }: { compact?: boolean }) {
  return (
    <Link href="/" className="brand" aria-label="Ovlo home">
      <BrandMark size={compact ? 30 : 34} />
      <span className="brand__wordmark">Ovlo</span>
    </Link>
  )
}
