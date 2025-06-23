package com.example.auth_service.security;

import java.util.Date;
import java.util.HashMap;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.example.auth_service.repository.TokenRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long jwtExpirationInMs;

    @Value("${jwt.refreshExpiration}")
    private long refreshExpirationInMs;

    private final TokenRepository tokenRepository;

    public JwtTokenProvider(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(String email) {
        return generateToken(email, jwtExpirationInMs);
    }

    public String generateRefreshToken(String email) {
        String refreshToken = generateToken(email, refreshExpirationInMs);
        // Store the refresh token in Redis
        tokenRepository.saveRefreshToken(email, refreshToken, refreshExpirationInMs);
        return refreshToken;
    }

    public long getJwtExpirationInMs() {
        return jwtExpirationInMs;
    }

    public long getRefreshExpirationInMs() {
        return refreshExpirationInMs;
    }

    private String generateToken(String username, long expiration) {
        return Jwts.builder()
                .claims(new HashMap<>())
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String validateRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (claims.getExpiration().before(new Date())) {
                throw new ExpiredJwtException(null, claims, "Refresh token expired");
            }

            String username = claims.getSubject();
            String storedToken = tokenRepository.getRefreshToken(username);

            if (storedToken == null || !storedToken.equals(token)) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
            }

            return username;

        } catch (ExpiredJwtException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        } catch (SignatureException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token signature");
        } catch (MalformedJwtException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        } catch (UnsupportedJwtException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unsupported refresh token");
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token claims string is empty");
        }
    }

    public boolean validateToken(String token) {
        try {
            // Check if token is blacklisted
            if (tokenRepository.isBlacklisted(token)) {
                return false;
            }

            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException ex) {
            // Invalid JWT signature
        } catch (MalformedJwtException ex) {
            // Invalid JWT token
        } catch (ExpiredJwtException ex) {
            // Expired JWT token
        } catch (UnsupportedJwtException ex) {
            // Unsupported JWT token
        } catch (IllegalArgumentException ex) {
            // JWT claims string is empty
        }
        return false;
    }

    public void invalidateToken(String token) {
        long expiration = getExpirationDateFromToken(token).getTime() - System.currentTimeMillis();
        if (expiration > 0) {
            tokenRepository.addToBlacklist(token, expiration);
        }
    }
}