package me.yeonjae.ovlo.application.port.out.member;

public interface LegacyMemberErasurePort {
    long pendingCount();
    int eraseBatch(int limit);
}
