package com.eeum.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SignupRequest {

    @Schema(example = "yumin123")
    @NotBlank @Size(min = 4, max = 30)
    @Pattern(regexp = "^[a-zA-Z0-9_\\-]+$", message = "아이디는 영문/숫자/_/- 만 가능합니다.")
    private String loginId;

    @Schema(example = "P@ssw0rd!")
    @NotBlank @Size(min = 8, max = 64, message = "비밀번호는 8~64자여야 합니다.")
    private String password;

    @Schema(example = "yumin@naver.com")
    @NotBlank @Email
    private String email;

    @Schema(example = "이유민")
    @NotBlank @Size(min = 2, max = 20)
    private String nickname;
}
