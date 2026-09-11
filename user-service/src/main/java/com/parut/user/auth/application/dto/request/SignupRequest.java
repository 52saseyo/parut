package com.parut.user.auth.application.dto.request;

public record SignupRequest(
        String username,
        String password,
        String name,
        String slackId
) {}