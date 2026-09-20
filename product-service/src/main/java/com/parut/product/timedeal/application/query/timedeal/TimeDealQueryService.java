package com.parut.product.timedeal.application.query.timedeal;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.global.dto.ImageQuery;
import com.parut.product.global.dto.ImageQueryResult;
import com.parut.product.timedeal.application.authorization.TimeDealAuthorizationChecker;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealDetailResult;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealCursorResult;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealDetailView;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealPublicDetailResult;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealPublicDetailView;
import com.parut.product.timedeal.application.port.in.timedeal.TimeDealQueryUseCase;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealQueryRepository;
import com.parut.product.timedeal.application.port.out.image.ImageQueryPort;
import com.parut.product.timedeal.domain.timedeal.TimeDealStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TimeDealQueryService implements TimeDealQueryUseCase {

    private final TimeDealQueryRepository timeDealQueryRepository;
    private final ImageQueryPort imageQueryPort;
    private final TimeDealAuthorizationChecker authorizationChecker;

    @Override
    public TimeDealCursorResult<TimeDealPublicDetailResult> getPublicList(
            TimeDealStatus status,
            String cursor,
            UUID cursorId,
            int size
    ) {
        List<TimeDealPublicDetailView> views = timeDealQueryRepository.findPublicList(
                status, cursor, cursorId, size);
        boolean hasNext = views.size() > size;
        List<TimeDealPublicDetailView> page = hasNext ? views.subList(0, size) : views;
        List<TimeDealPublicDetailResult> content = page.stream()
                .map(view -> TimeDealPublicDetailResult.from(
                        view,
                        imageQueryPort.findImage(new ImageQuery(view.timeDealId())).imageUrl()))
                .toList();

        if (!hasNext) {
            return TimeDealCursorResult.withoutNextCursor(content);
        }

        TimeDealPublicDetailView lastView = page.get(page.size() - 1);
        return TimeDealCursorResult.of(content, lastView.startAt().toString(), lastView.timeDealId(), true);
    }

    @Override
    public TimeDealPublicDetailResult getPublicDetail(UUID timeDealId) {
        TimeDealPublicDetailView timeDealPublicDetailView = timeDealQueryRepository.findPublicDetailById(timeDealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TIME_DEAL_NOT_FOUND));
        ImageQueryResult imageQueryResult = imageQueryPort.findImage(new ImageQuery(timeDealId));
        return TimeDealPublicDetailResult.from(timeDealPublicDetailView, imageQueryResult.imageUrl());
    }

    @Override
    public TimeDealCursorResult<TimeDealPublicDetailResult> getSellerOwnedTimeDealList(
            UUID sellerId,
            String requesterRole,
            String cursor,
            UUID cursorId,
            int size
    ) {
        authorizationChecker.requireSellerOwner(sellerId, requesterRole, sellerId);
        List<TimeDealPublicDetailView> views = timeDealQueryRepository.findSellerOwnedTimeDealList(
                sellerId, cursor, cursorId, size);
        boolean hasNext = views.size() > size;
        List<TimeDealPublicDetailView> page = hasNext ? views.subList(0, size) : views;
        List<TimeDealPublicDetailResult> content = page.stream()
                .map(view -> TimeDealPublicDetailResult.from(
                        view,
                        imageQueryPort.findImage(new ImageQuery(view.timeDealId())).imageUrl()))
                .toList();

        if (!hasNext) {
            return TimeDealCursorResult.withoutNextCursor(content);
        }

        TimeDealPublicDetailView lastView = page.get(page.size() - 1);
        return TimeDealCursorResult.of(content, lastView.startAt().toString(), lastView.timeDealId(), true);
    }


    @Override
    public TimeDealDetailResult getDetail(UUID timeDealId) {
        TimeDealDetailView timeDealDetailView = timeDealQueryRepository.findDetailById(timeDealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TIME_DEAL_NOT_FOUND));
        ImageQueryResult imageQueryResult = imageQueryPort.findImage(new ImageQuery(timeDealId));
        return TimeDealDetailResult.from(timeDealDetailView, imageQueryResult.imageUrl());
    }
}
