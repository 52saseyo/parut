package com.parut.notification.global.common;

import java.util.Optional;

public enum UserRole {
    CUSTOMER,
    SELLER,
    ADMIN;

    public static Optional<UserRole> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
