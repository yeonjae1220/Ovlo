package me.yeonjae.ovlo.domain.office.model;

/** Stable, bounded diagnostic values; never contain question or model output text. */
public enum InquiryRejectionReason {
    NO_CANDIDATES, MODEL_NO_GROUNDS, MODEL_REFUSED, NO_CITATION,
    BAD_CITATION, VERIFY_DENIED, EVIDENCE_CHANGED
}
