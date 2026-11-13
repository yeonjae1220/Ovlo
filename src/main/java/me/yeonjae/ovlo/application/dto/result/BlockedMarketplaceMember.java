package me.yeonjae.ovlo.application.dto.result;

/** Only the identity needed to manage the current member's outgoing blocks. */
public record BlockedMarketplaceMember(long memberId, String nickname) {}
