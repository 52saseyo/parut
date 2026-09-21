package com.parut.notification.global.logging;

import com.parut.notification.global.constant.TraceIdConstants;
import org.slf4j.MDC;

public final class TraceIdContext {
    private TraceIdContext() {
    }

    public static void set(String traceId) {
        MDC.put(TraceIdConstants.TRACE_ID, traceId);
    }

    public static String currentTraceId() {
        return MDC.get(TraceIdConstants.TRACE_ID);
    }

    public static void clear() {
        MDC.remove(TraceIdConstants.TRACE_ID);
    }
}
