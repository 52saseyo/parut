package com.parut.product.timedeal.application.command.timedeal;

import com.parut.product.global.exception.BusinessException;
import com.parut.product.global.exception.ErrorCode;
import com.parut.product.timedeal.application.authorization.TimeDealAuthorizationChecker;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealCreateCommand;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealCreateResult;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealConvertCommand;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealUpdateCommand;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealUpdateResult;
import com.parut.product.timedeal.application.dto.timedeal.TimeDealDeleteCommand;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
                productStockAllocationPort,
                new TimeDealAuthorizationChecker()
        );
    }

    @Nested
    @DisplayName("타임딜 삭제")
    class Delete {
        private final UUID id = UUID.randomUUID();

        private TimeDeal existingTimeDeal() {
            TimeDeal timeDeal = TimeDeal.create(SELLER_ID, UUID.randomUUID(), null, "사과", "설명",
                    TimeDealProductGrade.UGLY, "안동", HARVESTED_DATE, 10_000L,
                    BigDecimal.valueOf(30), START_AT, END_AT, 10, CREATED_AT);

            ReflectionTestUtils.setField(timeDeal, "id", id);

            when(timeDealRepository.findById(id)).thenReturn(Optional.of(timeDeal));
            return timeDeal;
        }

        private TimeDealStock existingStock() {
            TimeDealStock stock = TimeDealStock.create(id, 100, 10);
            when(timeDealStockRepository.findByTimeDealId(id)).thenReturn(Optional.of(stock));
            return stock;
        }

        @Test
        void 판매자_본인은_타임딜과_재고를_함께_삭제한다() {
            TimeDeal timeDeal = existingTimeDeal();
            TimeDealStock stock = existingStock();
            timeDealCommandService.delete(new TimeDealDeleteCommand(id, SELLER_ID, "SELLER"));
            assertThat(timeDeal.isDeleted()).isTrue();
            assertThat(stock.isDeleted()).isTrue();
            assertThat(timeDeal.getDeletedBy()).isEqualTo(SELLER_ID.toString());
            assertThat(stock.getDeletedBy()).isEqualTo(SELLER_ID.toString());
            verify(timeDealRepository).save(timeDeal);
            verify(timeDealStockRepository).save(stock);
            verifyNoInteractions(productStockAllocationPort);
        }

        @Test
        void 관리자는_타인의_타임딜을_삭제하고_관리자_ID를_기록한다() {
            TimeDeal timeDeal = existingTimeDeal();
            TimeDealStock stock = existingStock();
            UUID adminId = UUID.randomUUID();
            timeDealCommandService.delete(new TimeDealDeleteCommand(id, adminId, "ADMIN"));
            assertThat(timeDeal.getDeletedBy()).isEqualTo(adminId.toString());
            assertThat(stock.getDeletedBy()).isEqualTo(adminId.toString());
        }

        @Test
        void 다른_판매자는_삭제할_수_없다() {
            TimeDeal timeDeal = existingTimeDeal();
            assertThatThrownBy(() -> timeDealCommandService.delete(
                    new TimeDealDeleteCommand(id, UUID.randomUUID(), "SELLER")))
                    .extracting("errorCode").isEqualTo(ErrorCode.TIME_DEAL_ACCESS_DENIED);
            assertThat(timeDeal.isDeleted()).isFalse();
            verifyNoInteractions(timeDealStockRepository);
        }

        @Test
        void 구매자는_소유자_ID가_같아도_삭제할_수_없다() {
            existingTimeDeal();
            assertThatThrownBy(() -> timeDealCommandService.delete(
                    new TimeDealDeleteCommand(id, SELLER_ID, "CUSTOMER")))
                    .extracting("errorCode").isEqualTo(ErrorCode.TIME_DEAL_ACCESS_DENIED);
            verifyNoInteractions(timeDealStockRepository);
        }

        @Test
        void 타임딜이_없으면_404다() {
            when(timeDealRepository.findById(id)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> timeDealCommandService.delete(
                    new TimeDealDeleteCommand(id, SELLER_ID, "SELLER")))
                    .extracting("errorCode").isEqualTo(ErrorCode.TIME_DEAL_NOT_FOUND);
            verifyNoInteractions(timeDealStockRepository);
        }

        @Test
        void 재고_정보가_없으면_타임딜을_삭제하지_못하고_예외가_발생한다() {
            TimeDeal timeDeal = existingTimeDeal();
            when(timeDealStockRepository.findByTimeDealId(id)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> timeDealCommandService.delete(
                    new TimeDealDeleteCommand(id, SELLER_ID, "SELLER")))
                    .extracting("errorCode").isEqualTo(ErrorCode.TIME_DEAL_STOCK_NOT_FOUND);
            assertThat(timeDeal.isDeleted()).isFalse();
            verify(timeDealRepository, never()).save(any());
        }

        @Test
        void 판매중이면_둘다_삭제하지_않는다() {
            TimeDeal timeDeal = existingTimeDeal();
            TimeDealStock stock = existingStock();
            timeDeal.activate(START_AT);
            assertThatThrownBy(() -> timeDealCommandService.delete(
                    new TimeDealDeleteCommand(id, SELLER_ID, "SELLER")))
                    .extracting("errorCode").isEqualTo(ErrorCode.TIME_DEAL_ACTIVE_DELETE_NOT_ALLOWED);
            assertThat(timeDeal.isDeleted()).isFalse();
            assertThat(stock.isDeleted()).isFalse();
        }

        @Test
        void 선점된_재고가_있으면_둘다_삭제하지_않는다() {
            TimeDeal timeDeal = existingTimeDeal();
            TimeDealStock stock = existingStock();
            stock.reserve(1);
            assertThatThrownBy(() -> timeDealCommandService.delete(
                    new TimeDealDeleteCommand(id, SELLER_ID, "SELLER")))
                    .extracting("errorCode").isEqualTo(ErrorCode.TIME_DEAL_STOCK_DELETE_NOT_ALLOWED);
            assertThat(timeDeal.isDeleted()).isFalse();
            assertThat(stock.isDeleted()).isFalse();
            verify(timeDealRepository, never()).save(any());
            verify(timeDealStockRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("직접 등록")
    class Create {

        private static TimeDealCreateCommand command(Integer maxPurchaseQuantity, Integer initialQuantity) {
            return command(maxPurchaseQuantity, initialQuantity, "SELLER");
        }

        private static TimeDealCreateCommand command(Integer maxPurchaseQuantity, Integer initialQuantity, String role) {
            return new TimeDealCreateCommand(
                    SELLER_ID,
                    role,
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

        @Test
        void 권한이_없으면_등록과_재고_저장을_시도하지_않는다() {
            assertThatThrownBy(() -> timeDealCommandService.create(command(10, 100, "CUSTOMER")))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_ACCESS_DENIED);
            verifyNoInteractions(timeDealRepository, timeDealStockRepository, productStockAllocationPort);
        }

        @Test
        void 역할이_없으면_등록_권한_검사를_통과하지_못한다() {
            assertThatThrownBy(() -> timeDealCommandService.create(command(10, 100, null)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_ACCESS_DENIED);
            verifyNoInteractions(timeDealRepository, timeDealStockRepository, productStockAllocationPort);
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

    @Nested
    @DisplayName("일반 상품 전환")
    class Convert {

        @Test
        void 권한이_없으면_일반_상품_재고를_할당하지_않는다() {
            TimeDealConvertCommand command = new TimeDealConvertCommand(
                    UUID.randomUUID(), 100, SELLER_ID, "CUSTOMER", BigDecimal.TEN,
                    START_AT, END_AT, 10, 10);
            assertThatThrownBy(() -> timeDealCommandService.convert(command))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.TIME_DEAL_ACCESS_DENIED);
            verifyNoInteractions(timeDealRepository, timeDealStockRepository, productStockAllocationPort);
        }
    }

    @Nested
    @DisplayName("타임딜 수정")
    class Update {

        private TimeDeal existingTimeDeal(UUID id) {
            TimeDeal timeDeal = TimeDeal.create(SELLER_ID, null, null, "사과", "설명",
                    TimeDealProductGrade.UGLY, "안동", HARVESTED_DATE, 10_000L,
                    BigDecimal.valueOf(30), START_AT, END_AT, 10, CREATED_AT);

            ReflectionTestUtils.setField(timeDeal, "id", id);

            when(timeDealRepository.findById(id)).thenReturn(Optional.of(timeDeal));
            return timeDeal;
        }

        private TimeDealUpdateCommand updateCommand(UUID id, UUID requesterId, String role) {
            return new TimeDealUpdateCommand(id, requesterId, role,
                    null, "수정 사과", null, null, null, null, null,
                    BigDecimal.valueOf(20), null, null, null);
        }

        @Test
        void 판매자_본인의_부분수정은_기존값을_유지하고_가격을_재계산한다() {
            UUID id = UUID.randomUUID();
            TimeDeal timeDeal = existingTimeDeal(id);
            Instant updatedAt = Instant.parse("2026-09-13T13:00:00Z");
            when(timeDealRepository.saveAndFlush(timeDeal)).thenAnswer(invocation -> {
                ReflectionTestUtils.setField(timeDeal, "updatedAt", updatedAt);
                return timeDeal;
            });
            TimeDealUpdateResult result = timeDealCommandService.update(updateCommand(id, SELLER_ID, "SELLER"));
            assertThat(result.timeDealId()).isEqualTo(id);
            assertThat(result.status()).isEqualTo(TimeDealStatus.SCHEDULED);
            assertThat(result.updatedAt()).isEqualTo(updatedAt);
            assertThat(timeDeal.getName()).isEqualTo("수정 사과");
            assertThat(timeDeal.getDealPrice()).isEqualTo(8_000L);
            assertThat(timeDeal.getOriginalPrice()).isEqualTo(10_000L);
            assertThat(timeDeal.getStartAt()).isEqualTo(START_AT);
            assertThat(timeDeal.getSellerId()).isEqualTo(SELLER_ID);
            verify(timeDealRepository).saveAndFlush(timeDeal);
            verifyNoInteractions(timeDealStockRepository, productStockAllocationPort);
        }

        @Test
        void 관리자는_다른_판매자의_타임딜을_수정한다() {
            UUID id = UUID.randomUUID();
            TimeDeal timeDeal = existingTimeDeal(id);
            when(timeDealRepository.saveAndFlush(timeDeal)).thenReturn(timeDeal);
            timeDealCommandService.update(updateCommand(id, UUID.randomUUID(), "ADMIN"));
            assertThat(timeDeal.getName()).isEqualTo("수정 사과");
            assertThat(timeDeal.getSellerId()).isEqualTo(SELLER_ID);
            verify(timeDealRepository).saveAndFlush(timeDeal);
        }

        @Test
        void 다른_판매자는_수정할_수_없고_객체도_변경되지_않는다() {
            UUID id = UUID.randomUUID();
            TimeDeal timeDeal = existingTimeDeal(id);
            assertThatThrownBy(() -> timeDealCommandService.update(
                    updateCommand(id, UUID.randomUUID(), "SELLER")))
                    .extracting("errorCode").isEqualTo(ErrorCode.TIME_DEAL_ACCESS_DENIED);
            assertThat(timeDeal.getName()).isEqualTo("사과");
            verify(timeDealRepository, never()).saveAndFlush(any());
        }

        @Test
        void 구매자는_소유자_ID가_같아도_수정하지_못한다() {
            UUID id = UUID.randomUUID();
            TimeDeal timeDeal = existingTimeDeal(id);
            assertThatThrownBy(() -> timeDealCommandService.update(
                    updateCommand(id, SELLER_ID, "CUSTOMER")))
                    .extracting("errorCode").isEqualTo(ErrorCode.TIME_DEAL_ACCESS_DENIED);
            assertThat(timeDeal.getName()).isEqualTo("사과");
            verify(timeDealRepository, never()).saveAndFlush(any());
        }

        @Test
        void 수정_대상이_없으면_찾을수없음_오류다() {
            UUID id = UUID.randomUUID();
            when(timeDealRepository.findById(id)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> timeDealCommandService.update(updateCommand(id, SELLER_ID, "SELLER")))
                    .extracting("errorCode").isEqualTo(ErrorCode.TIME_DEAL_NOT_FOUND);
            verify(timeDealRepository, never()).saveAndFlush(any());
        }

        @Test
        void 판매중인_타임딜은_수정할_수_없다() {
            UUID id = UUID.randomUUID();
            TimeDeal timeDeal = existingTimeDeal(id);
            timeDeal.activate(START_AT);
            assertThatThrownBy(() -> timeDealCommandService.update(updateCommand(id, SELLER_ID, "SELLER")))
                    .extracting("errorCode").isEqualTo(ErrorCode.TIME_DEAL_UPDATE_NOT_ALLOWED);
            assertThat(timeDeal.getName()).isEqualTo("사과");
            verify(timeDealRepository, never()).saveAndFlush(any());
        }
    }
}
