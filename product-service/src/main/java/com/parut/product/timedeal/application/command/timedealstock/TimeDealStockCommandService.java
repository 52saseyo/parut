package com.parut.product.timedeal.application.command.timedealstock;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.timedeal.application.authorization.TimeDealAuthorizationChecker;
import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockAdjustCommand;
import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockAdjustResult;
import com.parut.product.timedeal.application.port.in.timedealstock.TimeDealStockCommandUseCase;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealRepository;
import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockRepository;
import com.parut.product.timedeal.domain.common.TimeDealPolicy;
import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import com.parut.product.timedeal.domain.timedealstock.TimeDealStock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TimeDealStockCommandService implements TimeDealStockCommandUseCase {

    private final TimeDealRepository timeDealRepository;
    private final TimeDealStockRepository timeDealStockRepository;
    private final TimeDealPolicy timeDealPolicy;
    private final TimeDealAuthorizationChecker authorizationChecker;

    @Override
    @Transactional
    public TimeDealStockAdjustResult adjustStock(TimeDealStockAdjustCommand timeDealStockAdjustCommand) {
        // TODO: 재고를 Redis 기준으로 전환할 때에도 타임딜 존재 여부와 권한 검증을 위한
        //       TimeDeal 조회를 DB에서 유지할지, 상태 정보까지 Redis에 복제할지 결정한다.
        TimeDeal timeDeal = timeDealRepository.findById(timeDealStockAdjustCommand.timeDealId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TIME_DEAL_NOT_FOUND));

        authorizationChecker.requireSellerOwnerOrAdmin(
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

        // TODO: Redis를 실시간 재고의 기준으로 사용하면 Result를 Redis 연산 결과로 만들고,
        //       reserved/sold의 조회 기준과 DB 동기화 시점을 함께 정한다.
        return TimeDealStockAdjustResult.from(timeDealStock);
    }

    @Override
    public Void transferStock() {
        return null;
    }
}
