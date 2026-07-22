package com.julensserver.service;

import com.julensserver.domain.User;
import com.julensserver.dto.auth.LoginRequest;
import com.julensserver.dto.auth.LoginResponse;
import com.julensserver.exception.BusinessException;
import com.julensserver.exception.ErrorCode;
import com.julensserver.jwt.JwtProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.julensserver.repository.UserRepository;
import com.julensserver.dto.auth.SignUpRequest;
import com.julensserver.dto.auth.SignUpResponse;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtProvider jwtProvider){
        this.userRepository=userRepository;
        this.passwordEncoder=passwordEncoder;
        this.jwtProvider=jwtProvider;
    }

    public SignUpResponse signUp(SignUpRequest signUpRequest){
        if(userRepository.existsByEmail(signUpRequest.getEmail())){
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        String encodedPassword = passwordEncoder.encode(signUpRequest.getPassword());

        User user = new User(signUpRequest.getEmail(), encodedPassword, signUpRequest.getNickname());

        User savedUser = userRepository.save(user);

        return SignUpResponse.from(savedUser);
    }

    public LoginResponse login(LoginRequest loginRequest){
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(()-> new BusinessException(ErrorCode.INVALID_LOGIN));

        if(!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())){
            throw new BusinessException(ErrorCode.INVALID_LOGIN);
        }

        String token = jwtProvider.createAccessToken(user.getId(), user.getEmail());

        return LoginResponse.of(token,user);
    }


}
