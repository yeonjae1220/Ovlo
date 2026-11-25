'use client'

import LegalDocumentPage from './LegalDocumentPage'
import { PRIVACY_POLICY } from './legalContent'

export default function PrivacyPolicyPage() {
  return (
    <LegalDocumentPage
      documents={PRIVACY_POLICY}
      related={{ href: '/terms', labelKey: 'legal.viewTerms' }}
    />
  )
}
