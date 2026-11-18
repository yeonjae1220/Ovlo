package me.yeonjae.ovlo.domain.office.model;

/** Stable, bounded diagnostic values; never contain question or model output text. */
public enum InquiryRejectionReason {
    /** The office had no notice eligible as grounds for this language: a content gap, not a search miss. */
    NO_ELIGIBLE_NOTICES,
    /** Eligible notices existed but retrieval returned none of them. */
    NO_CANDIDATES, MODEL_NO_GROUNDS, MODEL_REFUSED, NO_CITATION,
    BAD_CITATION, VERIFY_DENIED, EVIDENCE_CHANGED
}
