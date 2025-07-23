package com.example.common.dto;

import java.util.List;
import java.util.UUID;

public record UserValidationRequest(
        String correlationId,
        List<UUID> userIds
) {}