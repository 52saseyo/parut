package com.parut.order.order.infrastructure.client;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.port.out.TimeDealClient;
import com.parut.order.order.application.port.out.dto.TimeDealInfo;
import com.parut.order.order.infrastructure.client.dto.TimeDealInfoApiResponse;
import com.parut.order.order.infrastructure.client.dto.TimeDealPurchaseCancelApiRequest;
import com.parut.order.order.infrastructure.client.dto.TimeDealPurchaseReserveApiRequest;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeignTimeDealClient implements TimeDealClient {

    private final TimeDealInternalFeignClient timeDealInternalFeignClient;

    @Override
    public TimeDealInfo getOrderInfo(UUID timeDealId) {
        try {
            TimeDealInfoApiResponse response = timeDealInternalFeignClient.getOrderInfo(timeDealId).data();
            return new TimeDealInfo(
                    response.timeDealId(),
                    response.productId(),
                    response.sellerId(),
                    response.productName(),
                    response.dealPrice()
            );
        } catch (FeignException.NotFound e) {
            throw new BusinessException(ErrorCode.PRODUCT_UNAVAILABLE);
        } catch (FeignException e) {
            log.warn("[TimeDealClient] 타임딜 조회 실패 timeDealId={}, status={}", timeDealId, e.status(), e);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public void reserveStock(UUID timeDealId, UUID orderId, UUID userId, int quantity) {
        try {
            timeDealInternalFeignClient.reserveStock(timeDealId, userId, new TimeDealPurchaseReserveApiRequest(orderId, quantity));
        } catch (FeignException.NotFound | FeignException.Conflict e) {
            throw new BusinessException(ErrorCode.STOCK_SHORTAGE);
        } catch (FeignException e) {
            log.warn("[TimeDealClient] 타임딜 재고 예약 실패 timeDealId={}, orderId={}, status={}",
                    timeDealId, orderId, e.status(), e);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public void restoreStock(UUID orderId, String reason) {
        try {
            timeDealInternalFeignClient.restoreStock(orderId, new TimeDealPurchaseCancelApiRequest(reason));
        } catch (FeignException e) {
            log.warn("[TimeDealClient] 타임딜 재고 해제 실패 orderId={}, status={}", orderId, e.status(), e);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }
}
