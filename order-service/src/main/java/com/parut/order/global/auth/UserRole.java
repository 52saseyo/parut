package com.parut.order.global.auth;

import java.util.Optional;

public enum UserRole {

    CUSTOMER,
    SELLER,
    ADMIN,
    SYSTEM;

    // 모르는 값을 어떤 응답으로 다룰지는 인터셉터가 정한다
    public static Optional<UserRole> parse(String value) {
        try {
            return Optional.of(valueOf(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
