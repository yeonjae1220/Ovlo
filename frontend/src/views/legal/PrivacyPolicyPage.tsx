'use client'

import LegalDocumentPage from './LegalDocumentPage'
import { PRIVACY_POLICY } from './legalContent'

type Props = { original?: boolean }

export default function PrivacyPolicyPage({ original = false }: Props) {
  return (
    <LegalDocumentPage
      documents={PRIVACY_POLICY}
      related={{ href: '/terms', labelKey: 'legal.viewTerms' }}
      path="/privacy"
      original={original}
    />
  )
}
