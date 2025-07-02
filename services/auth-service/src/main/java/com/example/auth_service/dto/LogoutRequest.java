package com.example.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LogoutRequest {

    @NotBlank(message = "Refresh token is required")
    private String refreshToken;

    @NotBlank(message = "Token is required")
    private String token;


}
