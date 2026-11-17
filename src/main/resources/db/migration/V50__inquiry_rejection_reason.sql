-- Existing inquiries intentionally retain NULL: their exact rejection path is unknown.
ALTER TABLE office_inquiry ADD COLUMN rejection_reason VARCHAR(32);
ALTER TABLE office_inquiry ADD CONSTRAINT chk_inquiry_rejection_reason CHECK (
    rejection_reason IS NULL OR (
        outcome IN ('NO_GROUNDS', 'ESCALATED') AND rejection_reason IN (
            'NO_CANDIDATES', 'MODEL_NO_GROUNDS', 'MODEL_REFUSED', 'NO_CITATION',
            'BAD_CITATION', 'VERIFY_DENIED', 'EVIDENCE_CHANGED'
        )
    )
);
