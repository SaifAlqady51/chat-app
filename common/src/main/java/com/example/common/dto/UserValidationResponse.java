package com.example.common.dto;


public record UserValidationResponse(
        String correlationId,
        boolean isValid,
        String message
) {}