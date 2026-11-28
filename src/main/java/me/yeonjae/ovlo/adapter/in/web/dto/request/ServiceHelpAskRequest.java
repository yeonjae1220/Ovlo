package me.yeonjae.ovlo.adapter.in.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** lang 은 ko·en·ja·zh·de·fr·vi. 비어 있거나 모르는 값이면 ko. */
public record ServiceHelpAskRequest(@NotBlank @Size(max = 1000) String question, @Size(max = 5) String lang) {
}
