package me.yeonjae.ovlo.adapter.in.web.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMemberProfileRequest(
        @NotBlank(message = "이름은 필수입니다")
        @Size(min = 1, max = 50, message = "이름은 1~50자 이내여야 합니다")
        String name,

        @Size(max = 100, message = "고향은 100자를 초과할 수 없습니다")
        String hometown,

        @NotBlank(message = "전공명은 필수입니다")
        @Size(max = 100, message = "전공명은 100자를 초과할 수 없습니다")
        String majorName,

        @NotBlank(message = "학위 유형은 필수입니다")
        String degreeType,

        @Min(value = 1, message = "학년은 1 이상이어야 합니다")
        @Max(value = 8, message = "학년은 8 이하여야 합니다")
        Integer gradeLevel
) {}
