package com.parut.order.settlement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.parut.order.global.config.JpaAuditingConfig;
import com.parut.order.delivery.application.port.in.DeliveryCompletionQueryUseCase;
import com.parut.order.order.application.OrderItemConfirmationService;
import com.parut.order.order.application.OrderItemQueryService;
import com.parut.order.order.domain.Order;
import com.parut.order.order.domain.OrderDeliveryGroup;
import com.parut.order.order.domain.OrderItem;
import com.parut.order.order.domain.OrderItemStatus;
import com.parut.order.order.domain.OrderType;
import com.parut.order.order.infrastructure.persistence.OrderDeliveryGroupRepository;
import com.parut.order.order.infrastructure.persistence.OrderItemRepository;
import com.parut.order.order.infrastructure.persistence.OrderRepository;
import com.parut.order.settlement.domain.Settlement;
import com.parut.order.settlement.domain.SettlementStatus;
import com.parut.order.settlement.infrastructure.persistence.SettlementRepository;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@Import({
        OrderItemConfirmationService.class,
        OrderItemQueryService.class,
        SettlementService.class,
        SettlementCompletionProcessor.class,
        JpaAuditingConfig.class
})
// 서비스 트랜잭션의 커밋과 롤백을 확인하므로 테스트 전체를 트랜잭션으로 감싸지 않는다.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SettlementConfirmationTransactionTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SELLER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private OrderItemConfirmationService orderItemConfirmationService;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private SettlementRepository settlementRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDeliveryGroupRepository orderDeliveryGroupRepository;

    @MockitoBean
    private DeliveryCompletionQueryUseCase deliveryCompletionQueryUseCase;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("수동 구매확정과 주문상품 정산을 같은 트랜잭션으로 반영한다")
    void 수동_구매확정_정산_성공() {
        OrderItem fixture = createOrderItem(15_000L, 2);

        orderItemConfirmationService.confirmOrderItem(
                fixture.getOrderId(), fixture.getId(), CUSTOMER_ID);

        OrderItem orderItem = orderItemRepository.findById(fixture.getId()).orElseThrow();
        Settlement settlement = findSettlement(orderItem.getId());
        assertThat(orderItem.getItemStatus()).isEqualTo(OrderItemStatus.CONFIRMED);
        assertThat(settlement.getOrderItemId()).isEqualTo(orderItem.getId());
        assertThat(settlement.getSalesAmount()).isEqualTo(30_000L);
        assertThat(settlement.getSettlementAmount()).isEqualTo(30_000L);
        assertThat(settlement.getEligibleAt()).isEqualTo(orderItem.getConfirmedAt());
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.PENDING);
    }

    @Test
    @DisplayName("자동 구매확정과 주문상품 정산을 같은 트랜잭션으로 반영한다")
    void 자동_구매확정_정산_성공() {
        OrderItem fixture = createOrderItem(15_000L, 2);
        Instant confirmationTime = Instant.parse("2026-09-20T00:00:01Z");
        when(deliveryCompletionQueryUseCase.getDeliveredAt(fixture.getDeliveryGroupId()))
                .thenReturn(Optional.of(confirmationTime.minus(Duration.ofDays(7)).minusSeconds(1)));

        orderItemConfirmationService.confirmEligibleOrderItem(
                fixture.getId(), confirmationTime, confirmationTime.minus(Duration.ofDays(7)));

        OrderItem orderItem = orderItemRepository.findById(fixture.getId()).orElseThrow();
        Settlement settlement = findSettlement(orderItem.getId());
        assertThat(orderItem.getItemStatus()).isEqualTo(OrderItemStatus.CONFIRMED);
        assertThat(orderItem.getConfirmedAt()).isEqualTo(confirmationTime);
        assertThat(settlement.getOrderItemId()).isEqualTo(orderItem.getId());
        assertThat(settlement.getSalesAmount()).isEqualTo(30_000L);
        assertThat(settlement.getSettlementAmount()).isEqualTo(30_000L);
        assertThat(settlement.getEligibleAt()).isEqualTo(confirmationTime);
        assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.PENDING);
    }

    @Test
    @DisplayName("자동 구매확정 정산 실패 시 구매확정도 롤백한다")
    void 자동_구매확정_정산_실패_롤백() {
        OrderItem fixture = createOrderItem(Long.MAX_VALUE, 2);
        Instant confirmationTime = Instant.parse("2026-09-20T00:00:01Z");
        when(deliveryCompletionQueryUseCase.getDeliveredAt(fixture.getDeliveryGroupId()))
                .thenReturn(Optional.of(confirmationTime.minus(Duration.ofDays(7)).minusSeconds(1)));

        assertThatThrownBy(() -> orderItemConfirmationService.confirmEligibleOrderItem(
                fixture.getId(), confirmationTime, confirmationTime.minus(Duration.ofDays(7))))
                .isInstanceOf(ArithmeticException.class);

        OrderItem orderItem = orderItemRepository.findById(fixture.getId()).orElseThrow();
        assertThat(orderItem.getItemStatus()).isEqualTo(OrderItemStatus.ORDERED);
        assertThat(orderItem.getConfirmedAt()).isNull();
        assertThat(settlementRepository.findAll())
                .noneMatch(settlement -> fixture.getId().equals(settlement.getOrderItemId()));
    }

    @Test
    @DisplayName("정산 금액 계산 실패 시 수동 구매확정도 롤백한다")
    void 수동_구매확정_정산_실패_롤백() {
        OrderItem fixture = createOrderItem(Long.MAX_VALUE, 2);

        assertThatThrownBy(() -> orderItemConfirmationService.confirmOrderItem(
                fixture.getOrderId(), fixture.getId(), CUSTOMER_ID))
                .isInstanceOf(ArithmeticException.class);

        OrderItem orderItem = orderItemRepository.findById(fixture.getId()).orElseThrow();
        assertThat(orderItem.getItemStatus()).isEqualTo(OrderItemStatus.ORDERED);
        assertThat(orderItem.getConfirmedAt()).isNull();
        assertThat(settlementRepository.findAll())
                .noneMatch(settlement -> orderItem.getId().equals(settlement.getOrderItemId()));
    }

    private Settlement findSettlement(UUID orderItemId) {
        return settlementRepository.findAll().stream()
                .filter(settlement -> orderItemId.equals(settlement.getOrderItemId()))
                .findFirst()
                .orElseThrow();
    }

    private OrderItem createOrderItem(long unitPrice, int quantity) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        return transaction.execute(status -> {
            Order order = orderRepository.save(Order.create(
                    "FIX-SETTLEMENT-" + UUID.randomUUID().toString().substring(0, 8),
                    CUSTOMER_ID,
                    OrderType.NORMAL,
                    "정산고객",
                    "01012345678",
                    "12345",
                    "서울시 테스트구",
                    "정산 테스트",
                    null,
                    10_000L,
                    3_000L,
                    "settlement-" + UUID.randomUUID()
            ));
            OrderDeliveryGroup deliveryGroup = OrderDeliveryGroup.create(
                    order.getId(), SELLER_ID, 10_000L, 3_000L);
            deliveryGroup.markPreparing();
            deliveryGroup.markShipped();
            deliveryGroup.markDelivered();
            orderDeliveryGroupRepository.save(deliveryGroup);

            OrderItem orderItem = OrderItem.create(
                    order.getId(),
                    deliveryGroup.getId(),
                    UUID.randomUUID(),
                    null,
                    "정산 테스트 상품",
                    "NORMAL",
                    "국내산",
                    LocalDate.of(2026, 9, 1),
                    "EA",
                    BigDecimal.ONE,
                    unitPrice,
                    unitPrice,
                    quantity
            );
            return orderItemRepository.save(orderItem);
        });
    }
}
