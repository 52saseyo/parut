package com.parut.product.timedeal.application.command.timedeal;

import com.parut.product.global.dto.ProductStockAllocateCommand;
import com.parut.product.global.dto.ProductStockAllocateResult;
import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealConvertCommand;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealCreateCommand;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealCreateResult;
import com.parut.product.timedeal.application.port.in.timedeal.TimeDealCommandUseCase;
import com.parut.product.timedeal.application.port.out.product.ProductStockAllocationPort;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealRepository;
import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockRepository;
import com.parut.product.timedeal.domain.common.TimeDealPolicy;
import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import com.parut.product.timedeal.domain.timedeal.TimeDealProductGrade;
import com.parut.product.timedeal.domain.timedealstock.TimeDealStock;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;


@Slf4j
@Service
@RequiredArgsConstructor
public class TimeDealCommandService implements TimeDealCommandUseCase {

    private final TimeDealRepository timeDealRepository;
    private final TimeDealStockRepository timeDealStockRepository;
    private final TimeDealPolicy timeDealPolicy;
    private final ProductStockAllocationPort productStockAllocationPort;


    // NOTE: 직접 등록이라 productId는 null이다 — 표시 정보는 판매자가 입력한 값이 그대로 스냅샷이 된다.
    @Override
    @Transactional
    public TimeDealCreateResult create(TimeDealCreateCommand timeDealCreateCommand) {
        // NOTE: now는 유즈케이스당 한 번만 만들어 모든 도메인 호출에 같은 값을 넘긴다.
        Instant now = Instant.now();

        TimeDeal timeDeal = TimeDeal.create(
                timeDealCreateCommand.sellerId(),
                null,
                timeDealCreateCommand.imageId(),
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
        // NOTE: now는 유즈케이스당 한 번만 만들어 모든 도메인 호출에 같은 값을 넘긴다.
        Instant now = Instant.now();

        ProductStockAllocateCommand productStockAllocateCommand = timeDealConvertCommand.toAllocateCommand();
        ProductStockAllocateResult allocatedResult = productStockAllocationPort.allocate(productStockAllocateCommand);

        TimeDeal timeDeal = TimeDeal.create(
                allocatedResult.sellerId(),
                allocatedResult.productId(),
                allocatedResult.imageId(),
                allocatedResult.productName(),
                allocatedResult.productDescription(),
                toProductGrade(allocatedResult.appearanceType()),
                allocatedResult.productOrigin(),
                allocatedResult.productHarvestedDate(),
                allocatedResult.price(),
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

        TimeDealStock timeDealStock = timeDealPolicy.allocateStock(savedTimeDeal, allocatedResult.quantity(), timeDealConvertCommand.lowStockThreshold());

        timeDealStockRepository.save(timeDealStock);

        log.info(
                "[TimeDeal] 일반 상품 전환 완료. timeDealId={}, productId={}, sellerId={}, quantity={}",
                savedTimeDeal.getId(),
                allocatedResult.productId(),
                allocatedResult.sellerId(),
                allocatedResult.quantity()
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
}

