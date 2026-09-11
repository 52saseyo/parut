package com.parut.product.global.common;

import java.util.Optional;

public final class AuditorContext {

    private static final ThreadLocal<String> CURRENT_AUDITOR = new ThreadLocal<>();

    private AuditorContext() {}

    public static void set(String auditorId) {
        CURRENT_AUDITOR.set(auditorId);
    }

    public static Optional<String> get() {
        return Optional.ofNullable(CURRENT_AUDITOR.get());
    }

    public static void clear() {
        CURRENT_AUDITOR.remove();
    }
}
