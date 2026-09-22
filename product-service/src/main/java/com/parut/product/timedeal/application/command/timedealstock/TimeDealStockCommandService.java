package com.parut.product.timedeal.application.command.timedealstock;

import com.parut.product.global.dto.ProductStockTransferCommand;
import com.parut.product.global.dto.ProductStockTransferResult;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.timedeal.application.authorization.TimeDealAuthorizationChecker;
import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockAdjustCommand;
import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockAdjustResult;
import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockTransferCommand;
import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockTransferResult;
import com.parut.product.timedeal.application.event.timedealstock.TimeDealStockAdjustedEvent;
import com.parut.product.timedeal.application.event.timedealstock.TimeDealStockTransferredEvent;
import com.parut.product.timedeal.application.port.in.timedealstock.TimeDealStockCommandUseCase;
import com.parut.product.timedeal.application.port.out.product.ProductStockPort;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealRepository;
import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockRepository;
import com.parut.product.timedeal.domain.common.TimeDealPolicy;
import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import com.parut.product.timedeal.domain.timedealstock.TimeDealStock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TimeDealStockCommandService implements TimeDealStockCommandUseCase {

    private final TimeDealRepository timeDealRepository;
    private final ProductStockPort productStockPort;
    private final TimeDealStockRepository timeDealStockRepository;
    private final TimeDealPolicy timeDealPolicy;
    private final TimeDealAuthorizationChecker authorizationChecker;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public TimeDealStockAdjustResult adjustStock(TimeDealStockAdjustCommand timeDealStockAdjustCommand) {
        // TODO: 재고를 Redis 기준으로 전환할 때에도 타임딜 존재 여부와 권한 검증을 위한
        //       TimeDeal 조회를 DB에서 유지할지, 상태 정보까지 Redis에 복제할지 결정한다.
        TimeDeal timeDeal = timeDealRepository.findById(timeDealStockAdjustCommand.timeDealId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TIME_DEAL_NOT_FOUND));

        authorizationChecker.requireSellerOwner(
                timeDealStockAdjustCommand.requesterId(),
                timeDealStockAdjustCommand.requesterRole(),
                timeDeal.getSellerId()
        );

        // TODO: 구매 선점·확정·취소 등 전체 재고 변경 경로를 Redis로 통일할 때
        //       이 DB 조회를 Redis 재고 조회로 전환하고, DB와 Redis의 초기화·복구 정책을 정한다.
        TimeDealStock timeDealStock = timeDealStockRepository
                .findByTimeDealId(timeDealStockAdjustCommand.timeDealId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TIME_DEAL_STOCK_NOT_FOUND));

        // TODO: Redis 전환 후에는 JPA 엔티티를 직접 변경하지 않고, Redis 원자 연산 또는 Lua Script로
        //       delta 검증과 available/reserved/sold 변경을 처리하도록 교체한다.
        timeDealPolicy.adjustStock(timeDeal, timeDealStock, timeDealStockAdjustCommand.quantity());
        publishAdjustedEvent(timeDealStock);

        // TODO: Redis를 실시간 재고의 기준으로 사용하면 Result를 Redis 연산 결과로 만들고,
        //       reserved/sold의 조회 기준과 DB 동기화 시점을 함께 정한다.
        return TimeDealStockAdjustResult.from(timeDealStock);
    }

    @Override
    @Transactional
    public TimeDealStockTransferResult transferStock(
            TimeDealStockTransferCommand timeDealStockTransferCommand
    ) {
        // TODO: 재고 이동 동시성 제어를 Redis로 통일할 때에도 타임딜 존재 여부와 권한 검증을 위한
        //       TimeDeal 조회를 DB에서 유지할지, 상태 정보까지 Redis에 복제할지 결정한다.
        TimeDeal timeDeal = timeDealRepository.findById(timeDealStockTransferCommand.timeDealId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TIME_DEAL_NOT_FOUND));

        authorizationChecker.requireSellerOwner(
                timeDealStockTransferCommand.requesterId(),
                timeDealStockTransferCommand.requesterRole(),
                timeDeal.getSellerId()
        );

        if (timeDeal.getProductId() == null
                || !timeDeal.getProductId().equals(timeDealStockTransferCommand.productId())) {
            throw new BusinessException(ErrorCode.TIME_DEAL_STOCK_TRANSFER_NOT_ALLOWED);
        }

        // TODO: Redis 전환 후에는 타임딜 재고를 DB에서 일반 조회하지 않고 Redis의 재고 상태를 기준으로
        //       상품 재고 이동과 타임딜 재고 이동의 원자성 및 실패 보상 방식을 설계한다.
        TimeDealStock timeDealStock = timeDealStockRepository
                .findByTimeDealId(timeDealStockTransferCommand.timeDealId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TIME_DEAL_STOCK_NOT_FOUND));

        // 일반상품 재고가 먼저 변경되므로 ACTIVE/ENDED/STOPPED 및 타임딜 재고 하한을 선검증한다.
        timeDealPolicy.validateStockAdjustment(
                timeDeal, timeDealStock, timeDealStockTransferCommand.quantity());

        ProductStockTransferCommand productStockTransferCommand = ProductStockTransferCommand.of(
                timeDealStockTransferCommand.productId(),
                timeDealStockTransferCommand.quantity(),
                timeDealStockTransferCommand.requesterId(),
                timeDealStockTransferCommand.requesterRole()
        );

        // TODO: 상품 재고와 타임딜 재고가 서로 다른 재고 저장소로 분리되면
        //       두 재고 이동의 원자성, 순서, 실패 시 보상 처리를 정의한다.
        ProductStockTransferResult productStockTransferResult =
                productStockPort.transfer(productStockTransferCommand);

        // TODO: Redis 원자 연산으로 전환 시 상품 재고 이동 결과를 반영한 타임딜 재고 delta 검증과 변경을
        //       하나의 일관된 동시성 처리 흐름으로 통합한다.
        timeDealPolicy.transferStock(
                timeDeal,
                timeDealStock,
                productStockTransferResult.quantity()
        );
        publishTransferredEvent(timeDealStock);
        return TimeDealStockTransferResult.from(
                timeDealStock,
                productStockTransferResult.quantity(),
                productStockTransferResult
        );
    }

    private void publishAdjustedEvent(TimeDealStock stock) {
        eventPublisher.publishEvent(new TimeDealStockAdjustedEvent(
                stock.getTimeDealId(), stock.getId(), stock.getAvailableQuantity()));
    }

    private void publishTransferredEvent(TimeDealStock stock) {
        eventPublisher.publishEvent(new TimeDealStockTransferredEvent(
                stock.getTimeDealId(), stock.getId(), stock.getAvailableQuantity()));
    }
}
