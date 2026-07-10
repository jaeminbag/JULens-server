package com.julensserver.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey secretKey;
    private final long expiration;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value(("${jwt.expiration")) long expiration
    ){
        this.secretKey=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration=expiration;
    }

    public String createAccessToken(Long userId, String email){
        Date now = new Date();
        Date expiredAt = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiredAt)
                .signWith(secretKey)
                .compact();
    }

    public Long getUserId(String token){
        Claims claims = parseClaims(token);
        return Long.valueOf(claims.getSubject());
    }

    public boolean validateToken(String token){
        try{
            parseClaims(token);
            return true;
        }
        catch (Exception e){
            return false;
        }
    }

    public Claims parseClaims(String token){
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }


}
