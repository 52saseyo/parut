package com.parut.order.payment.infrastructure.client;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.payment.application.port.out.TimeDealStockConfirmClient;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeignTimeDealStockConfirmClient implements TimeDealStockConfirmClient {

    private final TimeDealStockConfirmFeignClient timeDealStockConfirmFeignClient;

    @Override
    public void confirmStock(UUID orderId) {
        try {
            timeDealStockConfirmFeignClient.confirmStock(orderId);
        } catch (FeignException.NotFound | FeignException.Conflict e) {
            // 구매 이력이 없거나(비정상) 예약이 만료됨(TIME_DEAL_RESERVATION_EXPIRED) -> 재고 확정 실패로 간주
            // 만료의 경우 Product가 이미 구매 이력 취소와 재고 복구를 커밋한 상태
            throw new BusinessException(ErrorCode.STOCK_SHORTAGE);
        } catch (FeignException e) {
            log.warn("[TimeDealStockConfirmClient] 타임딜 재고 확정 실패 orderId={}, status={}", orderId, e.status(), e);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }
}
