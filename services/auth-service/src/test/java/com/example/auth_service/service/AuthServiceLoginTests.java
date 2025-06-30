package com.example.auth_service.service;

import com.example.auth_service.dto.LoginRequest;
import com.example.auth_service.dto.AuthResponse;
import com.example.auth_service.entity.User;
import com.example.auth_service.repository.UserRepository;
import com.example.auth_service.repository.TokenRepository;
import com.example.auth_service.security.JwtTokenProvider;
import com.example.auth_service.service.guard.AuthGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceLoginTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private AuthGuard authGuard;

    @InjectMocks
    private AuthServiceImpl authService;

    private LoginRequest validLoginRequest;
    private User validUser;

    @BeforeEach
    void setUp() {
        validLoginRequest = new LoginRequest("user@example.com", "password123");
        validUser = User.builder()
                .email("user@example.com")
                .passwordHash("encodedPassword")
                .username("testuser")
                .id(UUID.randomUUID())
                .build();
    }

    @Test
    void login_Successful() {
        // Arrange
        when(authGuard.checkLoginAndGetUser(validLoginRequest))
                .thenReturn(Optional.of(validUser));
        when(jwtTokenProvider.generateToken(validUser.getEmail())).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(validUser.getEmail())).thenReturn("refreshToken");
        when(jwtTokenProvider.getJwtExpirationInMs()).thenReturn(3600000L);
        when(jwtTokenProvider.getRefreshExpirationInMs()).thenReturn(86400000L);

        // Act
        AuthResponse response = authService.login(validLoginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("accessToken", response.getToken());
        assertEquals("refreshToken", response.getRefreshToken());
        assertEquals(validUser.getEmail(), response.getEmail());
        assertEquals(validUser.getId(), response.getUserId());
        assertEquals(validUser.getUsername(), response.getUsername());
        assertEquals(validUser.getAvatarUrl(), response.getAvatarUrl());
        assertTrue(response.getExpiresAt().isAfter(Instant.now()));

        verify(authGuard).checkLoginAndGetUser(validLoginRequest);
        verify(tokenRepository).saveRefreshToken(
                validUser.getEmail(),
                "refreshToken",
                86400000L
        );
    }
}