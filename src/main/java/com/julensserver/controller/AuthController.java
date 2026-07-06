package com.julensserver.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.julensserver.dto.auth.SignUpRequest;
import com.julensserver.service.AuthService;
import com.julensserver.dto.common.*;
import com.julensserver.dto.auth.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService){
        this.authService=authService;
    }

    @PostMapping("/signup")
    public ApiResponse<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest signUpRequest){
        SignUpResponse signUpResponse = authService.signUp(signUpRequest);

        return ApiResponse.success("회원가입에 성공했습니다.", signUpResponse);
    }




}
