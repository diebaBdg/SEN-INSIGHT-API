package com.seninsight.backend.auth.dtos;

public record LoginRequest(
        String login,
        String password
) {}