package me.yeonjae.ovlo.application.port.out.office;

public interface OfficePersonalDataPort {
    void eraseMember(long memberId);
    int eraseExpiredBatch();
}
