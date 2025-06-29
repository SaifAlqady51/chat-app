package com.example.auth_service.service;

import com.example.auth_service.dto.AuthResponse;
import com.example.auth_service.dto.RegisterRequest;
import com.example.auth_service.entity.User;
import com.example.auth_service.repository.UserRepository;
import com.example.auth_service.security.JwtTokenProvider;
import com.example.auth_service.service.guard.AuthGuard;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import jakarta.validation.Validator;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceRegisterTests {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AuthGuard authGuard;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = RegisterRequest.builder()
                .email("test@example.com")
                .password("ValidPassword123!")
                .username("testuser")
                .build();
    }

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    @Test
    void register_WithValidRequest_ReturnsAuthResponse() {
        UUID userId = UUID.randomUUID();
        // Arrange
        User savedUser = User.builder()
                .id(userId)
                .email(validRequest.getEmail())
                .username(validRequest.getUsername())
                .status("online")
                .build();

        when(passwordEncoder.encode(any())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.generateToken(any())).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refreshToken");
        when(jwtTokenProvider.getJwtExpirationInMs()).thenReturn(3600000L);

        // Act
        AuthResponse response = authService.register(validRequest);

        // Assert
        assertNotNull(response);
        assertEquals("accessToken", response.getToken());
        assertEquals("refreshToken", response.getRefreshToken());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("online", response.getStatus());
        assertTrue(response.getExpiresAt().isAfter(Instant.now()));

        verify(authGuard).checkRegistrationEligibility(validRequest);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_WhenEmailAlreadyExists_ThrowsConflictException() {
        // Arrange
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists"))
                .when(authGuard).checkRegistrationEligibility(validRequest);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authService.register(validRequest));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Email already exists", exception.getReason());

        verify(authGuard).checkRegistrationEligibility(validRequest);
        verifyNoInteractions(userRepository);
    }
    @Test
    void validRequest_ShouldPassValidation() {
        RegisterRequest request = new RegisterRequest(
                "test@example.com",
                "username",
                "password123"
        );

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @ParameterizedTest
    @MethodSource("invalidRequestsProvider")
    void invalidRequests_ShouldFailValidation(
            RegisterRequest request,
            String expectedErrorMessage
    ) {
        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty(), "Expected validation errors but found none");
        assertTrue(
                violations.stream()
                        .anyMatch(v -> v.getMessage().equals(expectedErrorMessage)),
                "Expected error message: " + expectedErrorMessage +
                        " but found: " + violations
        );
    }

    private static Stream<Arguments> invalidRequestsProvider() {
        return Stream.of(
                Arguments.of(
                        new RegisterRequest("", "username", "password"),
                        "Email is required"
                ),
                Arguments.of(
                        new RegisterRequest("invalid-email", "username", "password"),
                        "Invalid email format"
                ),
                Arguments.of(
                        new RegisterRequest("test@example.com", "", "password"),
                        "Username is required"
                ),
                Arguments.of(
                        new RegisterRequest("test@example.com", "username", ""),
                        "Password is required"
                )
        );
    }

}