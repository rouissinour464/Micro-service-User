package com.pfe.auth.security;

import com.pfe.auth.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;   // ✅ IMPORTANT
import java.util.Date;

@Component
public class JwtUtils {

    private static final String SECRET_STRING = "MySuperSecretKeyForJwtTokenGeneration2026!!!";

    // ✅ La clé doit être un SecretKey, PAS java.security.Key
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_STRING.getBytes());

    private static final long EXPIRATION = 3600000; // 1 hour

    public String generateToken(String email, Role role) {

        return Jwts.builder()
                .subject(email)
                .claim("role", role.getName().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean isTokenValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    private Claims getClaims(String token) {

        return Jwts.parser()          // ✅ parser() est correct en 0.12.x
                .verifyWith(SECRET_KEY)   // ✅ MAINTENANT ÇA MARCHE
                .build()
                .parseSignedClaims(token) // ✅ Remplace parseClaimsJws
                .getPayload();            // ✅ Remplace getBody()
    }
}