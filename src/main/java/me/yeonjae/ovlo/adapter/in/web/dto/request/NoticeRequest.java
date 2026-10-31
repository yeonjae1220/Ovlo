package me.yeonjae.ovlo.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import me.yeonjae.ovlo.domain.office.model.NoticeCategory;

import java.time.LocalDate;
import java.util.List;

/** 공지 등록·수정 공통 본문. 날짜 순서(expiresAt > effectiveFrom)는 도메인이 검사한다. */
public record NoticeRequest(@NotNull NoticeCategory category,
                            @NotBlank @Size(max = 300) String title,
                            @NotBlank @Size(max = 20000) String body,
                            @Size(max = 1000) String sourceUrl,
                            @Size(max = 20) List<Long> attachmentMediaIds,
                            @NotBlank @Pattern(regexp = "\\d{4}-[12S]", message = "적용 학기는 2027-1 형식입니다") String term,
                            @NotNull LocalDate effectiveFrom,
                            @NotNull LocalDate expiresAt) {
}
