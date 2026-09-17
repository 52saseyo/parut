package com.parut.product.timedeal.application.query.timedealstock;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.timedeal.application.authorization.TimeDealAuthorizationChecker;
import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockQueryResult;
import com.parut.product.timedeal.application.port.in.timedealstock.TimeDealStockQueryUseCase;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealRepository;
import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockRepository;
import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import com.parut.product.timedeal.domain.timedealstock.TimeDealStock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimeDealStockQueryService implements TimeDealStockQueryUseCase {

    private final TimeDealRepository timeDealRepository;
    private final TimeDealStockRepository timeDealStockRepository;
    private final TimeDealAuthorizationChecker authorizationChecker;

    @Override
    public TimeDealStockQueryResult getStock(UUID timeDealId, UUID requesterId, String requesterRole) {
        TimeDeal timeDeal = timeDealRepository.findById(timeDealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TIME_DEAL_NOT_FOUND));
        authorizationChecker.requireSellerOwnerOrAdmin(requesterId, requesterRole, timeDeal.getSellerId());

        TimeDealStock timeDealStock = timeDealStockRepository.findByTimeDealId(timeDealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TIME_DEAL_STOCK_NOT_FOUND));

        return TimeDealStockQueryResult.from(timeDealStock);
    }
}
