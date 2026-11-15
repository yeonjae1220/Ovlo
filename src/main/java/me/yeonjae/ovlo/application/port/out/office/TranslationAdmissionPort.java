package me.yeonjae.ovlo.application.port.out.office;

/** Cumulative paid-call admission, separate from ownership of an individual translation. */
public interface TranslationAdmissionPort {
    int MAX_INPUT_BYTES = 65536;
    int MAX_OUTPUT_TOKENS = 2048;
    long RESERVATION = MAX_INPUT_BYTES + 5L * MAX_OUTPUT_TOKENS;
    /** Conservative full reservation remains charged even on timeout, rejection or process crash. */
    Lease acquire(Long officeId, Long responsibleMemberId);
    interface Lease extends AutoCloseable { @Override void close(); }
}
