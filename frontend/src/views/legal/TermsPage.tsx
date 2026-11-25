'use client'

import LegalDocumentPage from './LegalDocumentPage'
import { TERMS_OF_SERVICE } from './legalContent'

type Props = { original?: boolean }

export default function TermsPage({ original = false }: Props) {
  return (
    <LegalDocumentPage
      documents={TERMS_OF_SERVICE}
      related={{ href: '/privacy', labelKey: 'legal.viewPrivacy' }}
      path="/terms"
      original={original}
    />
  )
}
