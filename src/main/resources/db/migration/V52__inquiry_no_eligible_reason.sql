-- NO_CANDIDATES mixed "no eligible notice at all" with "search found none". Rows written before this
-- migration keep NO_CANDIDATES: which of the two they were is unknown.
ALTER TABLE office_inquiry DROP CONSTRAINT chk_inquiry_rejection_reason;
ALTER TABLE office_inquiry ADD CONSTRAINT chk_inquiry_rejection_reason CHECK (
    rejection_reason IS NULL OR (
        outcome IN ('NO_GROUNDS', 'ESCALATED') AND rejection_reason IN (
            'NO_ELIGIBLE_NOTICES', 'NO_CANDIDATES', 'MODEL_NO_GROUNDS', 'MODEL_REFUSED', 'NO_CITATION',
            'BAD_CITATION', 'VERIFY_DENIED', 'EVIDENCE_CHANGED'
        )
    )
);
