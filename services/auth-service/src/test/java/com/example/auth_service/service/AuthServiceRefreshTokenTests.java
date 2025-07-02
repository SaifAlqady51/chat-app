package com.example.auth_service.service;

import com.example.auth_service.dto.AuthResponse;
import com.example.auth_service.dto.ApiResponse;
import com.example.auth_service.dto.RefreshTokenRequest;
import com.example.auth_service.entity.User;
import com.example.auth_service.repository.UserRepository;
import com.example.auth_service.repository.TokenRepository;
import com.example.auth_service.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceRefreshTokenTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private RefreshTokenRequest validRefreshRequest;
    private User validUser;

    @BeforeEach
    void setUp() {
        validRefreshRequest = new RefreshTokenRequest("validRefreshToken");
        validUser = User.builder()
                .email("user@example.com")
                .username("testuser")
                .id(UUID.randomUUID())
                .status("ACTIVE")
                .build();
    }

    @Test
    void refreshToken_Successful() {
        // Arrange
        when(jwtTokenProvider.validateRefreshToken(validRefreshRequest.getRefreshToken()))
                .thenReturn(validUser.getEmail());
        when(userRepository.findByEmail(validUser.getEmail()))
                .thenReturn(Optional.of(validUser));
        when(jwtTokenProvider.generateToken(validUser.getEmail()))
                .thenReturn("newAccessToken");
        when(jwtTokenProvider.generateRefreshToken(validUser.getEmail()))
                .thenReturn("newRefreshToken");
        when(jwtTokenProvider.getJwtExpirationInMs()).thenReturn(3600000L);
        when(jwtTokenProvider.getRefreshExpirationInMs()).thenReturn(86400000L);

        // Act
        ApiResponse<AuthResponse> apiResponse = authService.refreshToken(validRefreshRequest);
        AuthResponse response = apiResponse.getData();

        // Assert
        assertNotNull(apiResponse);
        assertNotNull(response);
        assertEquals("newAccessToken", response.getToken());
        assertEquals("newRefreshToken", response.getRefreshToken());
        assertEquals(validUser.getEmail(), response.getEmail());
        assertEquals(validUser.getId(), response.getUserId());
        assertEquals(validUser.getUsername(), response.getUsername());
        assertEquals(validUser.getStatus(), response.getStatus());
        assertTrue(response.getExpiresAt().isAfter(Instant.now()));
        assertEquals(HttpStatus.OK.value(), apiResponse.getStatus());
        assertNotNull(apiResponse.getTimestamp());

        verify(jwtTokenProvider).validateRefreshToken(validRefreshRequest.getRefreshToken());
        verify(userRepository).findByEmail(validUser.getEmail());
        verify(tokenRepository).saveRefreshToken(
                validUser.getEmail(),
                "newRefreshToken",
                86400000L
        );
        verify(tokenRepository).addToBlacklist(
                validRefreshRequest.getRefreshToken(),
                86400000L
        );
    }

    @Test
    void refreshToken_InvalidRefreshToken_ThrowsUnauthorized() {
        // Arrange
        RefreshTokenRequest invalidRequest = new RefreshTokenRequest("invalidToken");
        when(jwtTokenProvider.validateRefreshToken(invalidRequest.getRefreshToken()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.refreshToken(invalidRequest));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("401 UNAUTHORIZED \"Invalid refresh token\"", exception.getMessage());
        verify(jwtTokenProvider).validateRefreshToken(invalidRequest.getRefreshToken());
        verifyNoInteractions(userRepository);
        verifyNoInteractions(tokenRepository);
    }

    @Test
    void refreshToken_UserNotFound_ThrowsUnauthorized() {
        // Arrange
        when(jwtTokenProvider.validateRefreshToken(validRefreshRequest.getRefreshToken()))
                .thenReturn("nonexistent@example.com");
        when(userRepository.findByEmail("nonexistent@example.com"))
                .thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.refreshToken(validRefreshRequest));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("401 UNAUTHORIZED \"User not found\"", exception.getMessage());
        verify(jwtTokenProvider).validateRefreshToken(validRefreshRequest.getRefreshToken());
        verify(userRepository).findByEmail("nonexistent@example.com");
        verifyNoInteractions(tokenRepository);
    }

}