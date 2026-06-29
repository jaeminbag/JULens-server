package com.julensserver.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class SignUpRequest {
    @Email(message = "이메일 형식이어야 합니다.")
    @Size(max = 30, message = "이메일은 30자 미만이어야 합니다.")
    private String email;

    @Size(min = 10,  max = 20, message = "비밀번호는 10자 이상, 20자 미만이어야 합니다.")
    private String password;

    @Size(min = 2, max = 20, message = "닉네임은 2자 이상, 20자 미만이어야 합니다.")
    private String nickname;

    public SignUpRequest(String email, String password, String nickname){
        this.email=email;
        this.password=password;
        this.nickname=nickname;
    }

    public String getEmail(){
        return email;
    }

    public String getPassword(){
        return password;
    }
    public String getNickname(){
        return nickname;
    }



}
