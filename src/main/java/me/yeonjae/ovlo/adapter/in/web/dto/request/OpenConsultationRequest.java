package me.yeonjae.ovlo.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** inquiryId 나 message 중 하나는 있어야 한다(유스케이스가 검사). consentVersion 은 학생이 본 동의 문구 버전. */
public record OpenConsultationRequest(Long inquiryId,
                                      @NotBlank @Size(max = 10) String consentVersion,
                                      @Size(max = 2000) String message) {
}
