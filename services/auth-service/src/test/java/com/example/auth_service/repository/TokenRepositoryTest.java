package com.example.auth_service.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class TokenRepositoryTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private TokenRepository tokenRepository;

    @Test
    void testSaveRefreshToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        tokenRepository.saveRefreshToken("user@example.com", "token123", 1000L);
        verify(valueOperations).set("refresh_token:user@example.com", "token123", 1000L, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    @Test
    void testGetRefreshToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh_token:user@example.com")).thenReturn("token123");
        String token = tokenRepository.getRefreshToken("user@example.com");
        assertEquals("token123", token);
    }

    @Test
    void testDeleteRefreshToken() {
        tokenRepository.deleteRefreshToken("user@example.com");
        verify(redisTemplate).delete("refresh_token:user@example.com");
    }

    @Test
    void testAddToBlacklist() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        tokenRepository.addToBlacklist("token123", 1000L);
        verify(valueOperations).set("blacklist:token123", "true", 1000L, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    @Test
    void testIsBlacklisted() {
        when(redisTemplate.hasKey("blacklist:token123")).thenReturn(true);
        assertTrue(tokenRepository.isBlacklisted("token123"));
    }
}