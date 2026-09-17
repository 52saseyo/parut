package com.parut.product.global.dto;

import java.util.UUID;

// NOTE: Image Service 조회 결과. 타임딜 응답에는 imageUrl만 노출하고 imageId는 내부 결과로만 보관한다.
public record ImageQueryResult(
        UUID imageId,
        String imageUrl
) {
}
