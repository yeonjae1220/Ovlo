package me.yeonjae.ovlo.domain.office.model;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** 승인 대상 원천. reference는 담당자 전용이며 학생 응답과 LLM 입력에 포함하지 않는다. */
public record NoticeSource(Type type, String department, String reference, String audience, String campus,
                           LocalDate acquiredOn, LocalDate publishedOn, Long ownerMemberId, boolean critical,
                           boolean publicSource, Long scopeId, List<Long> basisNoticeIds) {
    public enum Type { OFFICE_MATERIAL, UNIVERSITY_WEB, REVIEWED_STAFF_QA }
    public NoticeSource {
        Objects.requireNonNull(type, "출처 종류가 필요합니다");
        department = text(department, "발행 부서", 200);
        reference = text(reference, "원문 문서/담당자 확인 식별자", 1000);
        audience = text(audience, "적용 대상", 500);
        campus = text(campus, "적용 캠퍼스", 200);
        Objects.requireNonNull(acquiredOn, "자료 확보일이 필요합니다");
        if (ownerMemberId == null || ownerMemberId <= 0) throw new IllegalArgumentException("갱신 담당자가 필요합니다");
        if (type == Type.UNIVERSITY_WEB && (scopeId == null || !publicSource))
            throw new IllegalArgumentException("공식 홈페이지는 승인된 페이지 범위와 공개 출처가 필요합니다");
        basisNoticeIds = basisNoticeIds == null ? List.of() : List.copyOf(basisNoticeIds);
        if (basisNoticeIds.size() > 20 || basisNoticeIds.stream().anyMatch(id -> id == null || id <= 0)
                || basisNoticeIds.stream().distinct().count() != basisNoticeIds.size())
            throw new IllegalArgumentException("기반 자료는 중복 없이 최대 20개입니다");
    }
    public LocalDate nextReview(LocalDate today, LocalDate expiresAt) {
        LocalDate next = today.plusDays(critical ? 7 : 30);
        return next.isAfter(expiresAt) ? expiresAt : next;
    }
    public static String text(String value, String name, int max) {
        if (value == null || value.isBlank() || value.length() > max)
            throw new IllegalArgumentException(name + " 값이 없거나 너무 깁니다");
        return value.trim();
    }
    public static URI publicUri(String url) {
        try {
            URI uri = URI.create(url);
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null
                    || url.contains("\\") || uri.getRawPath().contains("%")) throw new IllegalArgumentException();
            return uri.normalize();
        } catch (RuntimeException e) { throw new IllegalArgumentException("유효한 HTTP(S) 출처 주소가 필요합니다"); }
    }
    public static boolean withinScope(String url, String scopeUrl) {
        try {
            URI u = publicUri(url), s = publicUri(scopeUrl);
            String p = s.getPath().replaceAll("/+$", "");
            return u.getScheme().equalsIgnoreCase(s.getScheme()) && u.getHost().equalsIgnoreCase(s.getHost())
                    && u.getPort() == s.getPort() && (u.getPath().equals(p) || u.getPath().startsWith(p + "/"));
        } catch (IllegalArgumentException e) { return false; }
    }
}
