package com.parut.user.user.application.dto.request;

public record UserUpdateRequest(
        String name,
        String slackId
) {
}