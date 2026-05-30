import type { Metadata } from 'next'
import './globals.css'
import { Providers } from './providers'

// eslint-disable-next-line react-refresh/only-export-components
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

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko">
      <body>
        <Providers>{children}</Providers>
      </body>
    </html>
  )
}
