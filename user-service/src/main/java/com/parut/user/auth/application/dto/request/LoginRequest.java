package com.parut.user.auth.application.dto.request;

public record LoginRequest(
        String username,
        String password
) {}