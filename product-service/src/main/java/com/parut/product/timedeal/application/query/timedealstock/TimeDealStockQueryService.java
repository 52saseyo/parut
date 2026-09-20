package com.parut.product.timedeal.application.query.timedealstock;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.timedeal.application.authorization.TimeDealAuthorizationChecker;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealCursorResult;
import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockListView;
import com.parut.product.timedeal.application.dto.timedealstock.TimeDealStockQueryResult;
import com.parut.product.timedeal.application.port.in.timedealstock.TimeDealStockQueryUseCase;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealRepository;
import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockRepository;
import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockQueryRepository;
import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import com.parut.product.timedeal.domain.timedealstock.TimeDealStock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimeDealStockQueryService implements TimeDealStockQueryUseCase {

    private final TimeDealRepository timeDealRepository;
    private final TimeDealStockRepository timeDealStockRepository;
    private final TimeDealStockQueryRepository timeDealStockQueryRepository;
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

    @Override
    public TimeDealCursorResult<TimeDealStockQueryResult> getSellerOwnedTimeDealStockList(
            UUID sellerId,
            String requesterRole,
            String cursor,
            UUID cursorId,
            int size
    ) {
        authorizationChecker.requireSellerOwner(sellerId, requesterRole, sellerId);
        List<TimeDealStockListView> views = timeDealStockQueryRepository
                .findSellerOwnedTimeDealStockList(sellerId, cursor, cursorId, size);
        boolean hasNext = views.size() > size;
        List<TimeDealStockListView> page = hasNext ? views.subList(0, size) : views;
        List<TimeDealStockQueryResult> content = page.stream()
                .map(view -> new TimeDealStockQueryResult(
                        view.timeDealId(),
                        view.availableQuantity(),
                        view.reservedQuantity(),
                        view.soldQuantity(),
                        view.lowStockThreshold()))
                .toList();

        if (!hasNext) {
            return TimeDealCursorResult.withoutNextCursor(content);
        }

        TimeDealStockListView lastView = page.get(page.size() - 1);
        return TimeDealCursorResult.of(content, lastView.startAt().toString(), lastView.timeDealId(), true);
    }
}
