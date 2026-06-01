package me.yeonjae.ovlo.adapter.in.web.dto.response;

import java.time.Instant;

public record ReadMarkerEvent(Long memberId, Instant lastReadAt) {}
