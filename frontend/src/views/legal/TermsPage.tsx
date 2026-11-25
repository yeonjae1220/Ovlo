'use client'

import LegalDocumentPage from './LegalDocumentPage'
import { TERMS_OF_SERVICE } from './legalContent'

export default function TermsPage() {
  return (
    <LegalDocumentPage
      documents={TERMS_OF_SERVICE}
      related={{ href: '/privacy', labelKey: 'legal.viewPrivacy' }}
    />
  )
}
