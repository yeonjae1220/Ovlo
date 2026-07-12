import type { Metadata } from 'next'
import { cookies, headers } from 'next/headers'
import './globals.css'
import { Providers } from './providers'
import { themeInitScript } from '@/theme/themeConfig'
import { resolveUiLang } from '@/i18n/messages'

export const dynamic = 'force-dynamic'

const BASE_URL = 'https://ovlo.mungji.com'

// eslint-disable-next-line react-refresh/only-export-components
export const metadata: Metadata = {
  metadataBase: new URL(BASE_URL),
  title: {
    default: 'Ovlo',
    template: '%s | Ovlo',
  },
  description: '교환학생 커뮤니티 — 대학교 정보, 게시판, 채팅을 한 곳에서',
  keywords: ['교환학생', '대학교', '커뮤니티', 'exchange student', 'university'],
  openGraph: {
    type: 'website',
    locale: 'ko_KR',
    url: BASE_URL,
    siteName: 'Ovlo',
    title: 'Ovlo — 교환학생 커뮤니티',
    description: '교환학생 커뮤니티 — 대학교 정보, 게시판, 채팅을 한 곳에서',
    images: [{ url: '/icons/og-image.png', width: 1200, height: 630, alt: 'Ovlo' }],
  },
  twitter: {
    card: 'summary_large_image',
    title: 'Ovlo — 교환학생 커뮤니티',
    description: '교환학생 커뮤니티 — 대학교 정보, 게시판, 채팅을 한 곳에서',
    images: ['/icons/og-image.png'],
  },
  robots: { index: true, follow: true },
}

export default async function RootLayout({ children }: { children: React.ReactNode }) {
  const [headerList, cookieStore] = await Promise.all([headers(), cookies()])
  const nonce = headerList.get('x-nonce') ?? ''
  const initialLanguage = resolveUiLang(cookieStore.get('ovlo_lang')?.value)
  return (
    <html lang={initialLanguage} data-theme="dark" suppressHydrationWarning>
      <head>
        {/* 페인트 전에 테마 적용(플래시 방지). :root 기본 dark 안전망과 함께 이중 방어. */}
        <script nonce={nonce} dangerouslySetInnerHTML={{ __html: themeInitScript }} />
      </head>
      <body data-nonce={nonce}>
        <Providers initialLanguage={initialLanguage}>{children}</Providers>
      </body>
    </html>
  )
}
