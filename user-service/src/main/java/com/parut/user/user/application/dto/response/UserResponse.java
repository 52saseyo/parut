package com.parut.user.user.application.dto.response;

import com.parut.user.user.domain.User;
import java.time.ZonedDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String name,
        String slackId,
        ZonedDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getSlackId(),
                user.getCreatedAt()
        );
    }
}