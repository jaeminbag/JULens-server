package com.julensserver.dto.auth;

import com.julensserver.domain.User;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class SignUpResponse {
    private Long userId;

    private String email;

    private String nickname;

    public static SignUpResponse from(User user){
        return new SignUpResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname()
        );
    }
}
