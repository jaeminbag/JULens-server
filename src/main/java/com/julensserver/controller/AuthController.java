package com.julensserver.controller;

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

    public AuthController(AuthService authService, AuthService authService_1){
        this.authService=authService;
    }





}
