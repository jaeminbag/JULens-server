package com.julensserver.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class LoginRequest {
    @Email(message = "이메일 형식이어야 합니다.")
    @Size(max = 30, message = "이메일은 30자 미만이어야 합니다.")
    private String email;

    @Size(min = 10,  max = 20, message = "비밀번호는 10자 이상, 20자 미만이어야 합니다.")
    private String password;

    public LoginRequest(String email, String password){
        this.email=email;
        this.password=password;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
