package com.julensserver.dto.auth;

import com.julensserver.domain.User;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginResponse {
    private String accessToken;
    private String tokenType;
    private Long userId;
    private String email;
    private String nickname;

    public static LoginResponse of(String accessToken, User user){
        return new LoginResponse(
                accessToken,
                "Bearer",
                user.getId(),
                user.getEmail(),
                user.getNickname()
        );
    }
}
