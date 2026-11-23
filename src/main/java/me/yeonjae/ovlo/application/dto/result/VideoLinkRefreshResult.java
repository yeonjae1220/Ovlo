package me.yeonjae.ovlo.application.dto.result;

/**
 * 영상 링크 재확인 한 회차의 결과 건수.
 *
 * @param hidden       사라진 것으로 보고 숨긴 건수
 * @param goneIgnored  '사라짐' 응답이 한 회차에 너무 많아 숨기지 않고 넘긴 건수(원본 쪽 차단 의심)
 */
public record VideoLinkRefreshResult(int refreshed, int hidden, int undecided, int failed,
                                     int goneIgnored, boolean stoppedEarly) {}
