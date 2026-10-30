'use client'

import LegalDocumentPage from './LegalDocumentPage'
import { TERMS_OF_SERVICE } from './legalContent'

export default function TermsPage() {
  return (
    <LegalDocumentPage
      documents={TERMS_OF_SERVICE}
      related={{ href: '/privacy', label: { ko: '개인정보처리방침 보기', en: 'View Privacy Policy' } }}
    />
  )
}
