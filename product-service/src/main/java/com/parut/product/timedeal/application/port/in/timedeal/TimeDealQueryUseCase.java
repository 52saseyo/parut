package com.parut.product.timedeal.application.port.in.timedeal;

import com.parut.product.timedeal.application.dto.timedeal.TimeDealDetailView;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealPublicDetailView;

import java.util.UUID;

public interface TimeDealQueryUseCase {

    // NOTE: 인증·소유권 검사 없이 누구나 조회하는 판매 조건과 재고 정보다.
    TimeDealPublicDetailView getPublicDetail(UUID timeDealId);

    // NOTE: 주문 생성 시 order-service가 결제 금액,판매자,표시 정보를 확보하려고 부른다.
    TimeDealDetailView getDetail(UUID timeDealId);
}
