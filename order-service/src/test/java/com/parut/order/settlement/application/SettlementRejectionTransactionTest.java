package com.parut.order.settlement.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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

import com.parut.order.delivery.application.port.in.DeliveryCompletionQueryUseCase;
import com.parut.order.global.config.JpaAuditingConfig;
import com.parut.order.order.application.OrderItemQueryService;
import com.parut.order.order.application.OrderItemRefundService;
import com.parut.order.order.domain.Order;
import com.parut.order.order.domain.OrderDeliveryGroup;
import com.parut.order.order.domain.OrderItem;
import com.parut.order.order.domain.OrderItemStatus;
import com.parut.order.order.domain.OrderType;
import com.parut.order.order.infrastructure.persistence.OrderDeliveryGroupRepository;
import com.parut.order.order.infrastructure.persistence.OrderItemRepository;
import com.parut.order.order.infrastructure.persistence.OrderRepository;
import com.parut.order.payment.application.port.in.PaymentCancelUseCase;
import com.parut.order.refund.application.RefundService;
import com.parut.order.refund.domain.Refund;
import com.parut.order.refund.domain.RefundStatus;
import com.parut.order.refund.infrastructure.persistence.RefundRepository;
import com.parut.order.settlement.domain.Settlement;
import com.parut.order.settlement.infrastructure.persistence.SettlementRepository;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
@Import({
        OrderItemRefundService.class,
        OrderItemQueryService.class,
        RefundService.class,
        SettlementService.class,
        SettlementCompletionProcessor.class,
        JpaAuditingConfig.class
})
// 서비스 트랜잭션의 커밋과 롤백을 확인하므로 테스트 전체를 트랜잭션으로 감싸지 않는다.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SettlementRejectionTransactionTest {

    private static final UUID CUSTOMER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SELLER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Autowired
    private RefundService refundService;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private SettlementRepository settlementRepository;

    @Autowired
    private RefundRepository refundRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderDeliveryGroupRepository orderDeliveryGroupRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private DeliveryCompletionQueryUseCase deliveryCompletionQueryUseCase;

    @MockitoBean
    private PaymentCancelUseCase paymentCancelUseCase;

    @Test
    @DisplayName("환불 거절과 주문상품 정산을 같은 트랜잭션으로 반영한다")
    void 환불_거절_정산_성공() {
        Refund fixture = createRefund(10_000L, 1);

        refundService.rejectRefund(fixture.getId(), SELLER_ID, "환불 거절 사유");

        Refund refund = refundRepository.findById(fixture.getId()).orElseThrow();
        OrderItem orderItem = getOrderItem(fixture);
        Settlement settlement = findSettlement(orderItem.getId());
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.REJECTED);
        assertThat(orderItem.getItemStatus()).isEqualTo(OrderItemStatus.CONFIRMED);
        assertThat(settlement.getOrderItemId()).isEqualTo(orderItem.getId());
        assertThat(settlement.getSettlementAmount()).isEqualTo(10_000L);
    }

    @Test
    @DisplayName("정산 금액 계산 실패 시 환불 거절과 구매확정을 함께 롤백한다")
    void 환불_거절_정산_실패_롤백() {
        Refund fixture = createRefund(Long.MAX_VALUE, 2);

        assertThatThrownBy(() -> refundService.rejectRefund(
                fixture.getId(), SELLER_ID, "환불 거절 사유"))
                .isInstanceOf(ArithmeticException.class);

        Refund refund = refundRepository.findById(fixture.getId()).orElseThrow();
        OrderItem orderItem = getOrderItem(fixture);
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.REQUESTED);
        assertThat(orderItem.getItemStatus()).isEqualTo(OrderItemStatus.REFUND_REQUESTED);
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

    private Refund createRefund(long unitPrice, int quantity) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        return transaction.execute(status -> {
            Order order = orderRepository.save(Order.create(
                    "FIX-REFUND-SETTLEMENT-" + UUID.randomUUID().toString().substring(0, 8),
                    CUSTOMER_ID,
                    OrderType.NORMAL,
                    "환불고객",
                    "01012345678",
                    "12345",
                    "서울시 테스트구",
                    "환불 거절 정산 테스트",
                    null,
                    10_000L,
                    3_000L,
                    "refund-settlement-" + UUID.randomUUID()
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
                    "환불 거절 정산 테스트 상품",
                    "NORMAL",
                    "국내산",
                    LocalDate.of(2026, 9, 1),
                    "EA",
                    BigDecimal.ONE,
                    unitPrice,
                    unitPrice,
                    quantity
            );
            orderItem.requestRefund();
            orderItemRepository.save(orderItem);

            return refundRepository.save(Refund.request(
                    orderItem.getId(), CUSTOMER_ID, SELLER_ID,
                    unitPrice, "상품 불량", Instant.now().minusSeconds(1)));
        });
    }

    private OrderItem getOrderItem(Refund refund) {
        return orderItemRepository.findById(refund.getOrderItemId()).orElseThrow();
    }

}
