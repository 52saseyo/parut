package com.parut.product.timedeal.application.query.timedeal;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.global.dto.ImageQuery;
import com.parut.product.global.dto.ImageQueryResult;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealDetailResult;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealPublicDetailResult;
import com.parut.product.timedeal.application.port.in.timedeal.TimeDealQueryUseCase;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealQueryRepository;
import com.parut.product.timedeal.application.port.out.image.ImageQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimeDealQueryService implements TimeDealQueryUseCase {

    private final TimeDealQueryRepository timeDealQueryRepository;
    private final ImageQueryPort imageQueryPort;

    @Override
    public TimeDealPublicDetailResult getPublicDetail(UUID timeDealId) {
        var view = timeDealQueryRepository.findPublicDetailById(timeDealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TIME_DEAL_NOT_FOUND));
        ImageQueryResult image = imageQueryPort.findImage(new ImageQuery(timeDealId));
        return TimeDealPublicDetailResult.from(view, image.imageUrl());
    }


    @Override
    public TimeDealDetailResult getDetail(UUID timeDealId) {
        var view = timeDealQueryRepository.findDetailById(timeDealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TIME_DEAL_NOT_FOUND));
        ImageQueryResult image = imageQueryPort.findImage(new ImageQuery(timeDealId));
        return TimeDealDetailResult.from(view, image.imageUrl());
    }
}
