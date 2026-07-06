package com.julensserver.dto.auth;

public class LoginResponse {
    private String accessToken;
    private String tokenType;
    private Long userId;
    private String email;
    private String nickname;

    public LoginResponse(String accessToken, String tokenType, Long userId, String email, String nickname){
        this.accessToken=accessToken;
        this.tokenType=tokenType;
        this.userId=userId;
        this.email=email;
        this.nickname=nickname;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getNickname() {
        return nickname;
    }
}
