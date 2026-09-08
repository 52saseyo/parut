package com.parut.user.user.application.dto.response;

import java.time.ZonedDateTime;
import java.util.UUID;

public record UserDeleteResponse(
        UUID userId,
        ZonedDateTime deletedAt
) {
    public static UserDeleteResponse of(UUID userId, ZonedDateTime deletedAt) {
        return new UserDeleteResponse(userId, deletedAt);
    }
}
