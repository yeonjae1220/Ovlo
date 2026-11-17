package me.yeonjae.ovlo.application.port.out.office;

public interface OfficePersonalDataPort {
    /** Caller transaction must hold this before office/evidence locks and personal-data writes. */
    void lockActiveMember(long memberId);
    void eraseMember(long memberId);
    int eraseExpiredBatch();
}
