package com.parut.product.timedeal.application.dto.timedeal;

import com.parut.product.timedeal.domain.timedeal.TimeDealProductGrade;

import java.time.LocalDate;
import java.util.UUID;


// NOTE: 조회 전용 프로젝션이라 애그리거트 루트를 거치지 않고 QueryRepository가 직접 채운다.
// NOTE: 표시 정보는 전부 타임딜 자기 컬럼(생성 시점 스냅샷)이라 상품 테이블을 join하지 않는다.
public record TimeDealDetailView(
        UUID timeDealId,
        UUID productId,
        UUID sellerId,
        UUID imageId,
        String productName,
        Long originalPrice,
        Long dealPrice,
        TimeDealProductGrade productGrade,
        String origin,
        LocalDate harvestedDate
) {
}