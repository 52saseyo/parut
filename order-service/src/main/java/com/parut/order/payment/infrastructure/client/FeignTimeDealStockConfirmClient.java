package com.parut.order.payment.infrastructure.client;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.payment.application.port.out.TimeDealStockConfirmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeignTimeDealStockConfirmClient implements TimeDealStockConfirmClient {

    private final TimeDealStockConfirmFeignClient timeDealStockConfirmFeignClient;

    @Override
    public void confirmStock(UUID orderId) {
        try {
            timeDealStockConfirmFeignClient.confirmStock(orderId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[TimeDealStockConfirmClient] 타임딜 재고 확정 실패 orderId={}", orderId, e);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }
}
