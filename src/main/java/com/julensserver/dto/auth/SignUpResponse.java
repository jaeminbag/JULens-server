package com.julensserver.dto.auth;

public class SignUpResponse {
    private Long userId;

    private String email;

    private String nickname;

    public SignUpResponse(Long userId, String email, String nickname){
        this.userId=userId;
        this.email=email;
        this.nickname=nickname;
    }

    public Long getUserId(){
        return userId;
    }

    public String getEmail(){
        return email;
    }

    public String nickname(){
        return nickname;
    }
}
