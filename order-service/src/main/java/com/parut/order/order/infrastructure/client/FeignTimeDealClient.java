package com.parut.order.order.infrastructure.client;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.port.out.TimeDealClient;
import com.parut.order.order.application.port.out.dto.TimeDealInfo;
import com.parut.order.order.infrastructure.client.dto.TimeDealBulkDetailApiRequest;
import com.parut.order.order.infrastructure.client.dto.TimeDealInfoApiResponse;
import com.parut.order.order.infrastructure.client.dto.TimeDealPurchaseCancelApiRequest;
import com.parut.order.order.infrastructure.client.dto.TimeDealPurchaseReserveApiRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FeignTimeDealClient implements TimeDealClient {

    private final TimeDealInternalFeignClient timeDealInternalFeignClient;

    @Override
    public TimeDealInfo getOrderInfo(UUID timeDealId) {
        try {
            List<TimeDealInfoApiResponse> responses = timeDealInternalFeignClient
                    .getOrderInfos(new TimeDealBulkDetailApiRequest(List.of(timeDealId)))
                    .data();
            TimeDealInfoApiResponse response = responses.get(0);
            return new TimeDealInfo(
                    response.timeDealId(),
                    response.productId(),
                    response.sellerId(),
                    response.productName(),
                    response.originalPrice(),
                    response.dealPrice(),
                    response.productGrade(),
                    response.origin(),
                    response.harvestedDate()
            );
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[TimeDealClient] 타임딜 조회 실패 timeDealId={}", timeDealId, e);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public void reserveStock(UUID timeDealId, UUID orderId, UUID userId, int quantity) {
        try {
            timeDealInternalFeignClient.reserveStock(timeDealId, userId, new TimeDealPurchaseReserveApiRequest(orderId, quantity));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[TimeDealClient] 타임딜 재고 예약 실패 timeDealId={}, orderId={}", timeDealId, orderId, e);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    @Override
    public void restoreStock(UUID orderId, String reason) {
        try {
            timeDealInternalFeignClient.restoreStock(orderId, new TimeDealPurchaseCancelApiRequest(reason));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[TimeDealClient] 타임딜 재고 해제 실패 orderId={}", orderId, e);
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE);
        }
    }
}
