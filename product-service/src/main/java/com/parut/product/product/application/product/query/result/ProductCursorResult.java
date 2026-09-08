package com.parut.product.product.application.product.query.result;

import java.util.List;
import java.util.UUID;

/**
 * Cursor 조회 결과를 application 계층 안에서 전달하기 위한 객체
 */
public record ProductCursorResult<T>(
        List<T> content,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext
) {
    /**
     * 다음 페이지가 존재하는 조회 결과를 생성
     */
    public static <T> ProductCursorResult<T> of(
            List<T> content,
            String nextCursor,
            UUID nextIdAfter,
            boolean hasNext
    ) {
        return new ProductCursorResult<>(content, nextCursor, nextIdAfter, hasNext);
    }

    /**
     * 다음 페이지가 없으므로 Cursor 정보를 비워 반환
     * */
    public static <T> ProductCursorResult<T> empty(List<T> content) {
        return new ProductCursorResult<>(content, null, null, false);
    }
}
