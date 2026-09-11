package com.parut.product.timedeal.application.port.in.timedeal;

import com.parut.product.timedeal.application.dto.timedeal.TimeDealDetailView;

import java.util.UUID;

public interface TimeDealQueryUseCase {

    // NOTE: 주문 생성 시 order-service가 결제 금액,판매자,표시 정보를 확보하려고 부른다.
    TimeDealDetailView getDetail(UUID timeDealId);
}
