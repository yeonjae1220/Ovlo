package me.yeonjae.ovlo.application.port.out.office;

/** Reserve before any paid screening, generation or verification, across all app replicas. */
public interface InquiryAdmissionPort {
    int MAX_INPUT_BYTES = 65_536;
    int MAX_OUTPUT_TOKENS = 2_048;
    int OUTPUT_WEIGHT = 5;
    long RESERVATION = 3L * (MAX_INPUT_BYTES + MAX_OUTPUT_TOKENS * OUTPUT_WEIGHT);
    Lease acquire(Long officeId, Long memberId);
    interface Lease extends AutoCloseable {
        void usage(long inputTokens, long outputTokens);
        /** Only refund unused reservation if all calls completed with known usage. */
        void complete();
        @Override void close();
    }
}
