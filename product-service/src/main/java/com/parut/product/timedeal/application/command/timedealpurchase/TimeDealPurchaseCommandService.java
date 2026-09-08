package com.parut.product.timedeal.application.command.timedealpurchase;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.timedeal.application.dto.timedealpurchase.TimeDealPurchaseCancelCommand;
import com.parut.product.timedeal.application.dto.timedealpurchase.TimeDealPurchaseConfirmCommand;
import com.parut.product.timedeal.application.dto.timedealpurchase.TimeDealPurchaseReserveCommand;
import com.parut.product.timedeal.application.exception.TimeDealReservationExpiredException;
import com.parut.product.timedeal.application.port.in.timedealpurchase.TimeDealPurchaseCommandUseCase;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealRepository;
import com.parut.product.timedeal.application.port.out.timedealpurchase.TimeDealPurchaseRepository;
import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockRepository;
import com.parut.product.timedeal.domain.common.TimeDealPolicy;
import com.parut.product.timedeal.domain.common.TimeDealPurchaseConfirmResult;
import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import com.parut.product.timedeal.domain.timedealpurchase.TimeDealPurchase;
import com.parut.product.timedeal.domain.timedealpurchase.TimeDealPurchaseStatus;
import com.parut.product.timedeal.domain.timedealstock.TimeDealStock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;


@Slf4j
@Service
@RequiredArgsConstructor
public class TimeDealPurchaseCommandService implements TimeDealPurchaseCommandUseCase {

    private final TimeDealRepository timeDealRepository;
    private final TimeDealStockRepository timeDealStockRepository;
    private final TimeDealPurchaseRepository timeDealPurchaseRepository;
    private final TimeDealPolicy timeDealPolicy;


    // NOTE: 애그리거트 조율은 전부 TimeDealPolicy를 경유한다. 여기 남는 것은 조회·저장·트랜잭션 경계와
    // 저장소 조회가 필요해 도메인이 지킬 수 없는 검증(orderId 중복, 누적 구매 수량 집계)뿐이다.
    @Override
    @Transactional
    public void reserve(TimeDealPurchaseReserveCommand timeDealPurchaseReserveCommand) {
        // NOTE: now는 유즈케이스당 한 번만 만들어 모든 도메인 호출에 같은 값을 넘긴다.
        Instant now = Instant.now();

        // NOTE: order_id unique 제약이 최종 방어선이고, 이 검사는 제약 위반(500)을 409로 바꿔준다.
        if (timeDealPurchaseRepository.existsByOrderId(timeDealPurchaseReserveCommand.orderId())) {
            throw new BusinessException(ErrorCode.TIME_DEAL_PURCHASE_ALREADY_EXISTS);
        }

        TimeDeal timeDeal = timeDealRepository.findById(timeDealPurchaseReserveCommand.timeDealId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TIME_DEAL_NOT_FOUND));
        TimeDealStock timeDealStock = timeDealStockRepository
                .findByTimeDealId(timeDealPurchaseReserveCommand.timeDealId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TIME_DEAL_STOCK_NOT_FOUND));

        // NOTE: 합산 대상이 RESERVED + CONFIRMED이고 CANCELLED가 빠지는 것은 어댑터가 보장한다.
        int alreadyPurchasedQuantity = timeDealPurchaseRepository.sumActiveQuantity(
                timeDealPurchaseReserveCommand.timeDealId(),
                timeDealPurchaseReserveCommand.userId()
        );

        TimeDealPurchase timeDealPurchase = timeDealPolicy.reserve(
                timeDeal,
                timeDealStock,
                timeDealPurchaseReserveCommand.orderId(),
                timeDealPurchaseReserveCommand.userId(),
                timeDealPurchaseReserveCommand.quantity(),
                alreadyPurchasedQuantity,
                now
        );

        // NOTE: 새로 만든 구매 이력만 저장한다. timeDeal/timeDealStock은 영속 상태라 변경분이 자동 반영된다.
        timeDealPurchaseRepository.save(timeDealPurchase);
    }


    // NOTE: noRollbackFor가 필요한 이유는 만료 정리(구매 이력 취소 + 재고 복구)를 커밋시켜야 하는데
    // BusinessException이 RuntimeException이라 그냥 던지면 그 정리까지 롤백되기 때문이다.
    @Override
    @Transactional(noRollbackFor = TimeDealReservationExpiredException.class)
    public void confirm(TimeDealPurchaseConfirmCommand timeDealPurchaseConfirmCommand) {
        Instant now = Instant.now();

        TimeDealPurchase timeDealPurchase = timeDealPurchaseRepository
                .findByOrderId(timeDealPurchaseConfirmCommand.orderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TIME_DEAL_PURCHASE_NOT_FOUND));
        TimeDealStock timeDealStock = timeDealStockRepository
                .findByTimeDealId(timeDealPurchase.getTimeDealId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TIME_DEAL_STOCK_NOT_FOUND));

        TimeDealPurchaseConfirmResult result =
                timeDealPolicy.confirmSale(timeDealPurchase, timeDealStock, now);

        // NOTE: 이 예외만 noRollbackFor에 지정되어 있어, 위 정리는 커밋되고 응답은 409가 나간다.
        if (result == TimeDealPurchaseConfirmResult.CANCELLED) {
            throw new TimeDealReservationExpiredException();
        }
    }


    @Override
    @Transactional
    public void cancel(TimeDealPurchaseCancelCommand timeDealPurchaseCancelCommand) {
        TimeDealPurchase timeDealPurchase = timeDealPurchaseRepository
                .findByOrderId(timeDealPurchaseCancelCommand.orderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TIME_DEAL_PURCHASE_NOT_FOUND));

        // NOTE: 멱등 처리 자체는 TimeDealPolicy가 하고, 여기서는 관측을 위해 로그만 남긴다.
        if (timeDealPurchase.getStatus() == TimeDealPurchaseStatus.CANCELLED) {
            log.warn(
                    "[TimeDealPurchase] 이미 취소된 구매에 대한 취소 요청. orderId={}, reason={}",
                    timeDealPurchaseCancelCommand.orderId(),
                    timeDealPurchaseCancelCommand.reason()
            );
        }

        TimeDealStock timeDealStock = timeDealStockRepository
                .findByTimeDealId(timeDealPurchase.getTimeDealId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TIME_DEAL_STOCK_NOT_FOUND));

        timeDealPolicy.cancelPurchase(
                timeDealPurchase, timeDealStock, timeDealPurchaseCancelCommand.reason());
    }
}
