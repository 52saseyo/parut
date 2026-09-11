package com.parut.product.timedeal.application.query.timedeal;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealDetailView;
import com.parut.product.timedeal.application.port.in.timedeal.TimeDealQueryUseCase;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealQueryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimeDealQueryService implements TimeDealQueryUseCase {

    private final TimeDealQueryRepository timeDealQueryRepository;


    @Override
    public TimeDealDetailView getDetail(UUID timeDealId) {
        return timeDealQueryRepository.findDetailById(timeDealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TIME_DEAL_NOT_FOUND));
    }
}