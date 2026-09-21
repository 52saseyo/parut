package com.parut.product.timedeal.application.query.timedeal;

import com.parut.product.global.dto.ImageQuery;
import com.parut.product.global.dto.ImageQueryResult;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.timedeal.application.authorization.TimeDealAuthorizationChecker;
import com.parut.product.timedeal.application.dto.timedeal.*;
import com.parut.product.timedeal.application.port.in.timedeal.TimeDealQueryUseCase;
import com.parut.product.timedeal.application.port.out.image.ImageQueryPort;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealQueryRepository;
import com.parut.product.timedeal.domain.timedeal.TimeDealStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


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

    @Override
    public List<TimeDealDetailResult> getDetailsByIds(List<UUID> timeDealIds) {
        List<TimeDealDetailView> views = timeDealQueryRepository.findDetailsByIds(timeDealIds);
        Map<UUID, TimeDealDetailView> viewsById = views.stream()
                .collect(Collectors.toMap(
                                view -> view.timeDealId(),
                                view -> view
                        )
                );

        if (viewsById.size() != timeDealIds.stream().distinct().count()) {
            throw new BusinessException(ErrorCode.TIME_DEAL_NOT_FOUND);
        }

        return timeDealIds.stream()
                .distinct()
                .map(timeDealId -> {
                    ImageQueryResult imageQueryResult = imageQueryPort.findImage(new ImageQuery(timeDealId)); // TODO: 추후 다건 이미지 조회로 변경
                    return TimeDealDetailResult.from(viewsById.get(timeDealId), imageQueryResult.imageUrl());
                })
                .toList();
    }
}
