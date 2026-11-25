package br.com.rafael.brindesmanager.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-ms}")
    private Long expirationMs;

    public String generateToken(CustomUserDetails userDetails) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("userId", userDetails.getId())
                .claim("name", userDetails.getName())
                .claim("role", userDetails.getUser().getRole().name())
                .claim("companyId", userDetails.getCompanyId())
                .claim("companyName", userDetails.getCompanyName())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Long userId = claims.get("userId", Long.class);
        if (userId == null || userId <= 0 || claims.getExpiration() == null) {
            throw new JwtException("Token sem identidade ou expiração válida");
        }
        return userId;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}