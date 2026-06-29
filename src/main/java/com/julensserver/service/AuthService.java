package com.julensserver.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.julensserver.repository.UserRepository;
import com.julensserver.dto.*;
import com.julensserver.dto.auth.SignUpRequest;
import com.julensserver.dto.auth.SignUpResponse;;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder){
        this.userRepository=userRepository;
        this.passwordEncoder=passwordEncoder;
    }



}
