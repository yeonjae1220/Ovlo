package me.yeonjae.ovlo.domain.moderation.model;

/** 신고 사유. OTHER 는 상세 설명이 있어야 관리자가 판단할 수 있다. */
public enum ReportReason {
    SPAM,
    HARASSMENT,
    HATE,
    SEXUAL,
    VIOLENCE,
    ILLEGAL,
    PERSONAL_INFO,
    OTHER
}
