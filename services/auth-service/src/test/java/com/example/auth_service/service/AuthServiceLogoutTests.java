package com.example.auth_service.service;

import com.example.auth_service.dto.LogoutRequest;
import com.example.auth_service.dto.ApiResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceLogoutTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private LogoutRequest validLogoutRequest;
    private User validUser;

    @BeforeEach
    void setUp() {
        validLogoutRequest = new LogoutRequest(
                "accessToken",
                "refreshToken"
        );

        validUser = User.builder()
                .email("user@example.com")
                .status("online")
                .id(UUID.randomUUID())
                .build();
    }

    @Test
    void logout_Successful() {
        // Arrange
        when(jwtTokenProvider.validateRefreshToken(validLogoutRequest.getRefreshToken()))
                .thenReturn(validUser.getEmail());
        when(userRepository.findByEmail(validUser.getEmail()))
                .thenReturn(Optional.of(validUser));
        when(jwtTokenProvider.getJwtExpirationInMs()).thenReturn(3600000L);
        when(jwtTokenProvider.getRefreshExpirationInMs()).thenReturn(86400000L);

        // Act
        ApiResponse<Void> response = authService.logout(validLogoutRequest);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK.value(), response.getStatus());
        assertEquals("Logout successful", response.getMessage());
        verify(userRepository).save(validUser);
        assertEquals("offline", validUser.getStatus());

        verify(tokenRepository).addToBlacklist(
                validLogoutRequest.getToken(),
                3600000L
        );
        verify(tokenRepository).addToBlacklist(
                validLogoutRequest.getRefreshToken(),
                86400000L
        );
    }


    @Test
    void logout_InvalidRefreshToken_ThrowsUnauthorized() {
        // Arrange
        when(jwtTokenProvider.validateRefreshToken(validLogoutRequest.getRefreshToken()))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.logout(validLogoutRequest));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("401 UNAUTHORIZED \"Invalid refresh token\"", exception.getMessage());
        verifyNoInteractions(userRepository);
        verifyNoInteractions(tokenRepository);
    }

    @Test
    void logout_UserNotFound_ThrowsNotFound() {
        // Arrange
        when(jwtTokenProvider.validateRefreshToken(validLogoutRequest.getRefreshToken()))
                .thenReturn("nonexistent@example.com");
        when(userRepository.findByEmail("nonexistent@example.com"))
                .thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.logout(validLogoutRequest));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("404 NOT_FOUND \"User not found\"", exception.getMessage());
        verify(tokenRepository, never()).addToBlacklist(anyString(), anyLong());
    }

}