package com.parut.product.global.constant;

public final class TraceIdConstants {

    // MDC에서 사용하는 키. HTTP 헤더 이름(X-Trace-Id)과는 구분한다.
    public static final String TRACE_ID = "traceId";

    private TraceIdConstants() {
    }
}
