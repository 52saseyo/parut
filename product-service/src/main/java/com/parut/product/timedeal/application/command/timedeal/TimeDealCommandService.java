package com.parut.product.timedeal.application.command.timedeal;

import com.parut.product.global.common.AuditorContext;
import com.parut.product.global.constant.AuditorConstants;
import com.parut.product.global.dto.ProductStockAllocateCommand;
import com.parut.product.global.dto.ProductStockAllocateResult;
import com.parut.product.global.dto.TimeDealImageSaveCommand;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.timedeal.application.authorization.TimeDealAuthorizationChecker;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealConvertCommand;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealCreateCommand;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealCreateResult;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealDeleteCommand;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealUpdateCommand;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealUpdateResult;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealStopCommand;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealStopResult;
import com.parut.product.timedeal.application.port.in.timedeal.TimeDealCommandUseCase;
import com.parut.product.timedeal.application.port.out.image.TimeDealImageCommandPort;
import com.parut.product.timedeal.application.port.out.product.ProductStockPort;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealRepository;
import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockRepository;
import com.parut.product.timedeal.application.event.timedealstock.TimeDealStockCreatedEvent;
import com.parut.product.timedeal.application.event.timedealstock.TimeDealStockDeletedEvent;
import com.parut.product.timedeal.domain.common.TimeDealPolicy;
import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import com.parut.product.timedeal.domain.timedeal.TimeDealProductGrade;
import com.parut.product.timedeal.domain.timedealstock.TimeDealStock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeDealCommandService implements TimeDealCommandUseCase {

    private final TimeDealSalePeriodProcessor timeDealSalePeriodProcessor;
    private final TimeDealRepository timeDealRepository;
    private final TimeDealStockRepository timeDealStockRepository;
    private final TimeDealPolicy timeDealPolicy;
    private final ProductStockPort productStockPort;
    private final TimeDealAuthorizationChecker authorizationChecker;
    private final TimeDealImageCommandPort timeDealImageCommandPort;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void endTimeDeals() {
        Instant now = Instant.now();
        UUID afterId = null;
        while (true) {
            List<UUID> ids = timeDealRepository.findTimeDealsToEnd(now, afterId, 100);
            if (ids.isEmpty()) {
                return;
            }
            processTimeDeals(ids);
            afterId = ids.getLast();
        }
    }

    @Override
    public void activateTimeDeals() {
        Instant now = Instant.now();
        UUID afterId = null;
        while (true) {
            List<UUID> ids = timeDealRepository.findTimeDealsToActivate(now, afterId, 100);
            if (ids.isEmpty()) {
                return;
            }
            processTimeDeals(ids);
            afterId = ids.getLast();
        }
    }

    private void processTimeDeals(List<UUID> ids) {
        for (UUID id : ids) {
            try {
                AuditorContext.set(AuditorConstants.BATCH_SYSTEM_USER_ID);
                // 대상 조회 이후 시간이 흐르거나 판매 조건이 바뀔 수 있어 처리 시점에 재판정한다.
                timeDealSalePeriodProcessor.synchronize(id);
            } catch (Exception e) {
                log.error("[TimeDeal] 판매 기간 상태 변경 실패: timeDealId={}", id, e);
            } finally {
                AuditorContext.clear();
            }
        }
    }

    @Override
    @Transactional
    public void delete(TimeDealDeleteCommand command) {
        TimeDeal timeDeal = timeDealRepository.findByIdForUpdate(command.timeDealId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TIME_DEAL_NOT_FOUND));
        authorizationChecker.requireSellerOwner(
                command.requesterId(), command.requesterRole(), timeDeal.getSellerId());
        TimeDealStock stock = timeDealStockRepository.findByTimeDealId(command.timeDealId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TIME_DEAL_STOCK_NOT_FOUND));

        // NOTE: 타임딜과 재고를 함께 soft delete한다. 일반 상품 재고 반환은 별도 유스케이스다.
        timeDealPolicy.delete(timeDeal, stock, command.requesterId().toString());
        timeDealRepository.save(timeDeal);
        timeDealStockRepository.save(stock);
        publishDeletedEvent(stock);
    }

    @Override
    @Transactional
    public TimeDealStopResult stop(TimeDealStopCommand command) {
        TimeDeal timeDeal = timeDealRepository.findByIdForUpdate(command.timeDealId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TIME_DEAL_NOT_FOUND));
        authorizationChecker.requireSellerOwner(
                command.requesterId(), command.requesterRole(), timeDeal.getSellerId());

        // NOTE: ACTIVE 타임딜만 STOPPED로 전이한다. 상태 전이 규칙은 도메인이 담당한다.
        timeDeal.stop();
        TimeDeal savedTimeDeal = timeDealRepository.save(timeDeal);
        return TimeDealStopResult.from(savedTimeDeal);
    }

    @Override
    @Transactional
    public TimeDealUpdateResult update(TimeDealUpdateCommand command) {
        TimeDeal timeDeal = timeDealRepository.findByIdForUpdate(command.timeDealId())
                .orElseThrow(() -> new BusinessException(ErrorCode.TIME_DEAL_NOT_FOUND));
        authorizationChecker.requireSellerOwner(
                command.requesterId(), command.requesterRole(), timeDeal.getSellerId());

        Instant now = Instant.now();
        // NOTE: 단일 애그리거트 수정이므로 TimeDealPolicy의 조율 없이 도메인에 위임한다.
        timeDeal.update(
                command.name(), command.description(), command.productGrade(),
                command.origin(), command.harvestedDate(), command.originalPrice(), command.discountRate(),
                command.startAt(), command.endAt(), command.maxPurchaseQuantity(), now
        );
        // NOTE: flush 시 갱신되는 감사 필드 updatedAt을 응답에 반영한다.
        TimeDeal savedTimeDeal = timeDealRepository.saveAndFlush(timeDeal);
        return TimeDealUpdateResult.from(savedTimeDeal);
    }


    // NOTE: 직접 등록이라 productId는 null이다 — 표시 정보는 판매자가 입력한 값이 그대로 스냅샷이 된다.
    @Override
    @Transactional
    public TimeDealCreateResult create(TimeDealCreateCommand timeDealCreateCommand) {
        authorizationChecker.requireSeller(timeDealCreateCommand.requesterRole());
        // NOTE: now는 유즈케이스당 한 번만 만들어 모든 도메인 호출에 같은 값을 넘긴다.
        Instant now = Instant.now();

        TimeDeal timeDeal = TimeDeal.create(
                timeDealCreateCommand.sellerId(),
                null,
                timeDealCreateCommand.name(),
                timeDealCreateCommand.description(),
                timeDealCreateCommand.productGrade(),
                timeDealCreateCommand.origin(),
                timeDealCreateCommand.harvestedDate(),
                timeDealCreateCommand.originalPrice(),
                timeDealCreateCommand.discountRate(),
                timeDealCreateCommand.startAt(),
                timeDealCreateCommand.endAt(),
                timeDealCreateCommand.maxPurchaseQuantity(),
                now
        );

        // NOTE: 재고는 타임딜 ID로 참조하므로 먼저 저장해 ID를 확보해야 한다(저장 전에는 null).
        TimeDeal savedTimeDeal = timeDealRepository.save(timeDeal);

        // NOTE: maxPurchaseQuantity <= 초기 재고 검증이 여기 있는 이유는 두 값이 다른 애그리거트에 있기 때문이다.
        TimeDealStock timeDealStock = timeDealPolicy.allocateStock(
                savedTimeDeal,
                timeDealCreateCommand.initialQuantity(),
                timeDealCreateCommand.lowStockThreshold()
        );
        timeDealStockRepository.save(timeDealStock);
        publishCreatedEvent(timeDealStock);

        log.info(
                "[TimeDeal] 직접 등록 완료. timeDealId={}, sellerId={}, initialQuantity={}",
                savedTimeDeal.getId(),
                timeDealCreateCommand.sellerId(),
                timeDealCreateCommand.initialQuantity()
        );
        return TimeDealCreateResult.from(savedTimeDeal);
    }

    @Override
    @Transactional
    public TimeDealCreateResult convert(TimeDealConvertCommand timeDealConvertCommand) {
        authorizationChecker.requireSeller(timeDealConvertCommand.requesterRole());
        // NOTE: now는 유즈케이스당 한 번만 만들어 모든 도메인 호출에 같은 값을 넘긴다.
        Instant now = Instant.now();

        ProductStockAllocateCommand productStockAllocateCommand = timeDealConvertCommand.toAllocateCommand();
        ProductStockAllocateResult productStockAllocateResult =
                productStockPort.allocate(productStockAllocateCommand);
        TimeDeal timeDeal = TimeDeal.create(
                productStockAllocateResult.sellerId(),
                productStockAllocateResult.productId(),
                productStockAllocateResult.productName(),
                productStockAllocateResult.productDescription(),
                toProductGrade(productStockAllocateResult.appearanceType()),
                productStockAllocateResult.productOrigin(),
                productStockAllocateResult.productHarvestedDate(),
                productStockAllocateResult.price(),
                timeDealConvertCommand.discountRate(),
                timeDealConvertCommand.startAt(),
                timeDealConvertCommand.endAt(),
                timeDealConvertCommand.maxPurchaseQuantity(),
                now
        );
        // NOTE: 재시도로 같은 상품이 같은 기간으로 두 번 전환되면 일반 재고가 두 번 차감된다(되돌리는 흐름이 없다).
        // 유니크 제약 위반을 여기서 드러내 409로 바꾸고, 같은 트랜잭션이라 일반 재고 차감도 함께 롤백된다.
        TimeDeal savedTimeDeal;
        try {
            savedTimeDeal = timeDealRepository.saveAndFlush(timeDeal);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.TIME_DEAL_CONVERSION_ALREADY_EXISTS);
        }

        TimeDealStock timeDealStock = timeDealPolicy.allocateStock(
                savedTimeDeal, productStockAllocateResult.quantity(), timeDealConvertCommand.lowStockThreshold());

        timeDealStockRepository.save(timeDealStock);
        publishCreatedEvent(timeDealStock);

        if (productStockAllocateResult.imageId() != null) {
            timeDealImageCommandPort.save(new TimeDealImageSaveCommand(
                    savedTimeDeal.getId(), productStockAllocateResult.imageId()));
        }

        log.info(
                "[TimeDeal] 일반 상품 전환 완료. timeDealId={}, productId={}, sellerId={}, quantity={}",
                savedTimeDeal.getId(),
                productStockAllocateResult.productId(),
                productStockAllocateResult.sellerId(),
                productStockAllocateResult.quantity()
        );
        return TimeDealCreateResult.from(savedTimeDeal);
    }


    // NOTE: 경계에서 String으로 받으므로 여기가 유일한 방어선이다 — 미지의 값이면 500이 아니라 400으로 드러낸다.
    private static TimeDealProductGrade toProductGrade(String appearanceType) {
        if (appearanceType == null) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_PRODUCT_GRADE);
        }
        try {
            return TimeDealProductGrade.valueOf(appearanceType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.TIME_DEAL_INVALID_PRODUCT_GRADE);
        }
    }

    private void publishCreatedEvent(TimeDealStock stock) {
        eventPublisher.publishEvent(new TimeDealStockCreatedEvent(
                stock.getTimeDealId(), stock.getId(), stock.getAvailableQuantity()));
    }

    private void publishDeletedEvent(TimeDealStock stock) {
        eventPublisher.publishEvent(new TimeDealStockDeletedEvent(
                stock.getTimeDealId(), stock.getId()));
    }
}
