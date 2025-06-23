package com.example.auth_service.repository;
import org.springframework.stereotype.Repository;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.concurrent.TimeUnit;

@Repository

public class TokenRepository {
    private final RedisTemplate<String, Object> redisTemplate;

    // Constructor injection
    public TokenRepository(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Stores refresh token with TTL (Time To Live)
    public void saveRefreshToken(String email, String refreshToken, long expirationMs) {
        String key = "refresh_token:" + email;
        redisTemplate.opsForValue().set(
                key,
                refreshToken,
                expirationMs,
                TimeUnit.MILLISECONDS
        );
    }

    // Retrieves refresh token for a user
    public String getRefreshToken(String email) {
        return (String) redisTemplate.opsForValue().get("refresh_token:" + email);
    }

    // Deletes refresh token (on logout)
    public void deleteRefreshToken(String email) {
        redisTemplate.delete("refresh_token:" + email);
    }

    // Adds token to blacklist until it expires
    public void addToBlacklist(String token, long expirationMs) {
        redisTemplate.opsForValue().set(
                "blacklist:" + token,
                "true",
                expirationMs,
                TimeUnit.MILLISECONDS
        );
    }

    // Checks if token is blacklisted
    public boolean isBlacklisted(String token) {
        return redisTemplate.hasKey("blacklist:" + token);
    }
}