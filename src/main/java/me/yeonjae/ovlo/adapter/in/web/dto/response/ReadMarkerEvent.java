package me.yeonjae.ovlo.adapter.in.web.dto.response;

import java.time.LocalDateTime;

public record ReadMarkerEvent(Long memberId, LocalDateTime lastReadAt) {}
