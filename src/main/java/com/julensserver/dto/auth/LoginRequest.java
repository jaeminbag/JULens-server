package com.julensserver.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginRequest {
    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "이메일 형식이어야 합니다.")
    @Size(max = 30, message = "이메일은 30자 미만이어야 합니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 10,  max = 20, message = "비밀번호는 10자 이상, 20자 미만이어야 합니다.")
    private String password;
}
