package me.yeonjae.ovlo.adapter.in.web.dto.request;
import jakarta.validation.constraints.*;
import me.yeonjae.ovlo.application.dto.command.UniversityRegistrationCommand;
public record UniversityRegistrationRequest(
        @NotBlank @Size(min=2,max=30) @Pattern(regexp="^[a-zA-Z0-9._]+$") String nickname,
        @NotBlank @Size(max=100) String name,
        @NotBlank @Email @Size(max=254) String email,
        @NotBlank @Size(min=8,max=100)
        @Pattern(regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&_#^()\\-])[A-Za-z\\d@$!%*?&_#^()\\-]{8,100}$") String password,
        @NotNull @Positive Long universityId, @NotBlank @Size(max=200) String department,
        @NotBlank @Size(max=2000) String message) {
    public UniversityRegistrationCommand command() {
        return new UniversityRegistrationCommand(nickname, name, email, password, universityId, department, message);
    }
}
