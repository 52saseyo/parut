package com.parut.notification.notification.presentation.dto.response;

public record UnreadNotificationCountResponse(long count) {
    public static UnreadNotificationCountResponse from(long count) {
        return new UnreadNotificationCountResponse(count);
    }
}
