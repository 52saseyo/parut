package com.parut.product.timedeal.application.command.timedeal;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealCreateCommand;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealCreateResult;
import com.parut.product.timedeal.application.port.out.timedeal.TimeDealRepository;
import com.parut.product.timedeal.application.port.out.product.ProductStockAllocationPort;
import com.parut.product.timedeal.application.port.out.timedealstock.TimeDealStockRepository;
import com.parut.product.timedeal.domain.common.TimeDealPolicy;
import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import com.parut.product.timedeal.domain.timedeal.TimeDealProductGrade;
import com.parut.product.timedeal.domain.timedeal.TimeDealStatus;
import com.parut.product.timedeal.domain.timedealstock.TimeDealStock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// NOTE: Repository만 mock하고 TimeDealPolicy는 실물을 쓴다 — Policy를 mock하면 애그리거트 상태 결과가 가려진다.
@ExtendWith(MockitoExtension.class)
class TimeDealCommandServiceTest {

    private static final UUID SELLER_ID = UUID.randomUUID();
    private static final LocalDate HARVESTED_DATE = LocalDate.of(2026, 9, 1);
    private static final Instant CREATED_AT = Instant.parse("2026-08-30T10:10:00Z");
    private static final Instant START_AT = Instant.parse("2099-09-04T11:00:00Z");
    private static final Instant END_AT = Instant.parse("2099-09-04T12:00:00Z");
    private static final int INITIAL_QUANTITY = 100;
    private static final int MAX_PURCHASE_QUANTITY = 10;
    private static final int LOW_STOCK_THRESHOLD = 10;

    @Mock
    private TimeDealRepository timeDealRepository;

    @Mock
    private TimeDealStockRepository timeDealStockRepository;

    // NOTE: create() 경로는 이 포트를 타지 않는다. 전환 경로 테스트는 allocate() 연결 후에 붙인다.
    @Mock
    private ProductStockAllocationPort productStockAllocationPort;

    private TimeDealCommandService timeDealCommandService;

    @BeforeEach
    void setUp() {
        timeDealCommandService = new TimeDealCommandService(
                timeDealRepository,
                timeDealStockRepository,
                new TimeDealPolicy(),
                productStockAllocationPort
        );
    }

    private static TimeDealCreateCommand command(Integer maxPurchaseQuantity, Integer initialQuantity) {
        return new TimeDealCreateCommand(
                SELLER_ID,
                null,
                "산지직송 사과 5kg",
                "당일 수확한 사과입니다.",
                TimeDealProductGrade.UGLY,
                "경북 안동",
                HARVESTED_DATE,
                10_000L,
                BigDecimal.valueOf(30),
                START_AT,
                END_AT,
                maxPurchaseQuantity,
                initialQuantity,
                LOW_STOCK_THRESHOLD
        );
    }

    // NOTE: save()가 id와 createdAt(@CreatedDate)을 채워 돌려주는 JPA 동작을 흉내낸다 —
    // 재고가 그 id를 참조하므로 없으면 검증에 걸리고, 응답이 createdAt을 담는다.
    private UUID stubSaveAssigningId() {
        UUID timeDealId = UUID.randomUUID();
        when(timeDealRepository.save(any(TimeDeal.class))).thenAnswer(invocation -> {
            TimeDeal saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", timeDealId);
            ReflectionTestUtils.setField(saved, "createdAt", CREATED_AT);
            return saved;
        });
        return timeDealId;
    }

    @Test
    @DisplayName("판매자가 입력한 값으로 타임딜과 재고를 함께 생성하고 타임딜 ID를 돌려준다")
    void 직접_등록_성공() {
        UUID expectedTimeDealId = stubSaveAssigningId();

        TimeDealCreateResult result =
                timeDealCommandService.create(command(MAX_PURCHASE_QUANTITY, INITIAL_QUANTITY));

        // NOTE: 응답 계약 — 직접 등록이라 productId는 null이고 상태는 SCHEDULED로 시작한다.
        assertThat(result.timeDealId()).isEqualTo(expectedTimeDealId);
        assertThat(result.productId()).isNull();
        assertThat(result.status()).isEqualTo(TimeDealStatus.SCHEDULED);
        assertThat(result.startAt()).isEqualTo(START_AT);
        assertThat(result.endAt()).isEqualTo(END_AT);
        assertThat(result.createdAt()).isEqualTo(CREATED_AT);

        ArgumentCaptor<TimeDeal> timeDealCaptor = ArgumentCaptor.forClass(TimeDeal.class);
        verify(timeDealRepository).save(timeDealCaptor.capture());
        TimeDeal savedTimeDeal = timeDealCaptor.getValue();

        assertThat(savedTimeDeal.getSellerId()).isEqualTo(SELLER_ID);
        assertThat(savedTimeDeal.getProductId()).isNull();
        assertThat(savedTimeDeal.getName()).isEqualTo("산지직송 사과 5kg");
        assertThat(savedTimeDeal.getProductGrade()).isEqualTo(TimeDealProductGrade.UGLY);
        assertThat(savedTimeDeal.getOrigin()).isEqualTo("경북 안동");
        assertThat(savedTimeDeal.getHarvestedDate()).isEqualTo(HARVESTED_DATE);
        assertThat(savedTimeDeal.getDealPrice()).isEqualTo(7_000L);
        assertThat(savedTimeDeal.getStatus()).isEqualTo(TimeDealStatus.SCHEDULED);
    }

    @Test
    @DisplayName("생성된 재고는 저장된 타임딜 ID를 참조하고 초기 수량으로 시작한다")
    void 재고_할당() {
        UUID expectedTimeDealId = stubSaveAssigningId();

        timeDealCommandService.create(command(MAX_PURCHASE_QUANTITY, INITIAL_QUANTITY));

        ArgumentCaptor<TimeDealStock> stockCaptor = ArgumentCaptor.forClass(TimeDealStock.class);
        verify(timeDealStockRepository).save(stockCaptor.capture());
        TimeDealStock savedStock = stockCaptor.getValue();

        assertThat(savedStock.getTimeDealId()).isEqualTo(expectedTimeDealId);
        assertThat(savedStock.getAvailableQuantity()).isEqualTo(INITIAL_QUANTITY);
        assertThat(savedStock.getReservedQuantity()).isZero();
        assertThat(savedStock.getSoldQuantity()).isZero();
        assertThat(savedStock.getLowStockThreshold()).isEqualTo(LOW_STOCK_THRESHOLD);
    }

    @Test
    @DisplayName("1인당 최대 구매 수량이 초기 재고보다 크면 예외 — 재고는 저장되지 않는다")
    void 최대구매수량이_초기재고_초과() {
        stubSaveAssigningId();

        assertThatThrownBy(() -> timeDealCommandService.create(command(20, 10)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.TIME_DEAL_MAX_PURCHASE_QUANTITY_EXCEEDS_STOCK);

        verify(timeDealStockRepository, never()).save(any(TimeDealStock.class));
    }

    @Test
    @DisplayName("커맨드 필수값이 비면 저장 자체를 시도하지 않는다")
    void 필수값_누락() {
        assertThatThrownBy(() -> command(MAX_PURCHASE_QUANTITY, null))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE);

        verify(timeDealRepository, never()).save(any(TimeDeal.class));
    }
}
