import type { MetadataRoute } from 'next'

const BASE_URL = 'https://ovlo.mungji.com'

export default function robots(): MetadataRoute.Robots {
  return {
    rules: [
      {
        userAgent: '*',
        allow: ['/', '/exchange-universities', '/university-reports', '/login', '/register'],
        disallow: ['/boards/', '/posts/', '/profile/', '/chat/', '/search/', '/follow/', '/onboarding/'],
      },
    ],
    sitemap: `${BASE_URL}/sitemap.xml`,
  }
}
