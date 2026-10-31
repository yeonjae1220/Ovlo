'use client'

import LegalDocumentPage from './LegalDocumentPage'
import { PRIVACY_POLICY } from './legalContent'

export default function PrivacyPolicyPage() {
  return (
    <LegalDocumentPage
      documents={PRIVACY_POLICY}
      related={{ href: '/terms', label: { ko: '이용약관 보기', en: 'View Terms of Service' } }}
    />
  )
}
