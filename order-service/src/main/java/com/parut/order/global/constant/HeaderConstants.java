package com.parut.order.global.constant;

public final class HeaderConstants {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_ROLE = "X-User-Role";
    public static final String TRACE_ID = "X-Trace-Id";

    // NOTE: 서비스 간 내부 호출 인증용 공유 시크릿
    public static final String SERVICE_KEY = "X-Service-Key";

    private HeaderConstants() {
    }
}
