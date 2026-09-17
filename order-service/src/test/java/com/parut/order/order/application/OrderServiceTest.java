package com.parut.order.order.application;

import com.parut.order.global.auth.UserRole;
import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.dto.*;
import com.parut.order.order.application.port.out.dto.ProductOrderInfo;
import com.parut.order.order.application.port.out.dto.TimeDealInfo;
import com.parut.order.order.domain.*;
import com.parut.order.order.infrastructure.persistence.*;
import com.parut.order.payment.application.port.in.PaymentQueryUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final UUID USER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b1");
    private static final UUID PRODUCT_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b2");
    private static final UUID SELLER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b3");
    private static final UUID TIME_DEAL_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b4");
    private static final String IDEMPOTENCY_KEY = "idem-key-0001";

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderDeliveryGroupRepository orderDeliveryGroupRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private OrderCancelRepository orderCancelRepository;

    @Mock
    private PaymentQueryUseCase paymentQueryUseCase;

    @InjectMocks
    private OrderService orderService;

    private CreateOrderCommand createCommand(List<OrderItemCommand> items) {
        return new CreateOrderCommand(
                USER_ID, IDEMPOTENCY_KEY, items,
                "홍길동", "01012345678", "06234", "서울특별시 강남구 테헤란로 123", "5층 501호", null
        );
    }

    private CreateOrderCommand createCommand(int quantity) {
        return createCommand(List.of(new OrderItemCommand(PRODUCT_ID, quantity)));
    }

    private ProductOrderInfo purchasableProductInfo() {
        return productInfo(PRODUCT_ID, SELLER_ID);
    }

    private ProductOrderInfo productInfo(UUID productId, UUID sellerId) {
        return new ProductOrderInfo(
                productId, sellerId, "신고배 5kg 특품", "NORMAL", "국내산(전남 나주)",
                LocalDate.of(2026, 8, 20), "KG", BigDecimal.valueOf(5), 15_000L, true
        );
    }

    private void stubSaveMocks() {
        when(orderRepository.saveAndFlush(any(Order.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));
        when(orderDeliveryGroupRepository.save(any(OrderDeliveryGroup.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));
        when(orderItemRepository.save(any(OrderItem.class)))
                .thenAnswer(invocation -> withId(invocation.getArgument(0)));
    }

    private Order existingOrder(String orderNo) {
        return Order.create(
                orderNo, USER_ID, OrderType.NORMAL,
                "홍길동", "01012345678", "06234", "서울특별시 강남구 테헤란로 123", null, null,
                30_000L, 3_000L, IDEMPOTENCY_KEY
        );
    }

    private CreateTimeDealOrderCommand createTimeDealCommand(int quantity) {
        return new CreateTimeDealOrderCommand(
                USER_ID, IDEMPOTENCY_KEY, TIME_DEAL_ID, PRODUCT_ID, quantity,
                "홍길동", "01012345678", "06234", "서울특별시 강남구 테헤란로 123", "5층 501호", null
        );
    }

    private TimeDealInfo timeDealInfo() {
        return new TimeDealInfo(
                TIME_DEAL_ID, PRODUCT_ID, SELLER_ID, "신고배 5kg 특품(타임딜)",
                15_000L, 12_000L, "NORMAL", "국내산(전남 나주)", LocalDate.of(2026, 8, 20)
        );
    }

    private <T> T withId(T entity) {
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        return entity;
    }

    @Test
    @DisplayName("saveNewOrder는 주문·배송그룹·아이템을 저장하고 CREATED 이력을 남긴다")
    void 주문_저장() {
        stubSaveMocks();

        CreatedOrder result = orderService.saveNewOrder(
                createCommand(2), Map.of(PRODUCT_ID, purchasableProductInfo())
        );

        assertThat(result.order().getOrderStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(result.order().getTotalProductAmount()).isEqualTo(30_000L);
        assertThat(result.order().getTotalDeliveryFee()).isEqualTo(3_000L);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).getQuantity()).isEqualTo(2);
        verify(orderStatusHistoryRepository).save(any());
    }

    @Test
    @DisplayName("같은 판매자의 상품을 여러 개 담으면 배송그룹 1개에 아이템 여러 개로 저장된다")
    void 다건주문_같은판매자() {
        stubSaveMocks();
        UUID secondProductId = UUID.randomUUID();

        CreateOrderCommand command = createCommand(List.of(
                new OrderItemCommand(PRODUCT_ID, 2),
                new OrderItemCommand(secondProductId, 1)
        ));
        Map<UUID, ProductOrderInfo> productInfoByProductId = Map.of(
                PRODUCT_ID, productInfo(PRODUCT_ID, SELLER_ID),
                secondProductId, productInfo(secondProductId, SELLER_ID)
        );

        CreatedOrder result = orderService.saveNewOrder(command, productInfoByProductId);

        assertThat(result.items()).hasSize(2);
        assertThat(result.order().getTotalProductAmount()).isEqualTo(15_000L * 2 + 15_000L);
        assertThat(result.order().getTotalDeliveryFee()).isEqualTo(3_000L);
        verify(orderDeliveryGroupRepository, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    @DisplayName("다른 판매자의 상품을 담으면 배송그룹이 판매자 수만큼 생성되고 배송비도 그만큼 부과된다")
    void 다건주문_다른판매자() {
        stubSaveMocks();
        UUID secondProductId = UUID.randomUUID();
        UUID secondSellerId = UUID.randomUUID();

        CreateOrderCommand command = createCommand(List.of(
                new OrderItemCommand(PRODUCT_ID, 1),
                new OrderItemCommand(secondProductId, 1)
        ));
        Map<UUID, ProductOrderInfo> productInfoByProductId = Map.of(
                PRODUCT_ID, productInfo(PRODUCT_ID, SELLER_ID),
                secondProductId, productInfo(secondProductId, secondSellerId)
        );

        CreatedOrder result = orderService.saveNewOrder(command, productInfoByProductId);

        assertThat(result.items()).hasSize(2);
        assertThat(result.order().getTotalDeliveryFee()).isEqualTo(3_000L * 2);
        verify(orderDeliveryGroupRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    @DisplayName("판매자 수가 상한(5개)을 초과하면 TOO_MANY_SELLERS 예외를 던지고 아무것도 저장하지 않는다")
    void 다건주문_판매자수초과() {
        List<OrderItemCommand> items = java.util.stream.IntStream.range(0, 6)
                .mapToObj(i -> new OrderItemCommand(UUID.randomUUID(), 1))
                .toList();
        Map<UUID, ProductOrderInfo> productInfoByProductId = items.stream()
                .collect(Collectors.toMap(OrderItemCommand::productId, item -> productInfo(item.productId(), UUID.randomUUID())));
        CreateOrderCommand command = createCommand(items);

        assertThatThrownBy(() -> orderService.saveNewOrder(command, productInfoByProductId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.TOO_MANY_SELLERS);

        verify(orderDeliveryGroupRepository, org.mockito.Mockito.never()).save(any());
        verify(orderItemRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("saveNewTimeDealOrder는 타임딜 조회 API가 주는 스냅샷 필드를 채우고, 미제공 필드만 null로 저장한다")
    void 타임딜주문_저장() {
        stubSaveMocks();

        CreatedOrder result = orderService.saveNewTimeDealOrder(createTimeDealCommand(2), timeDealInfo());

        assertThat(result.order().getOrderType()).isEqualTo(OrderType.TIME_DEAL);
        assertThat(result.order().getOrderStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(result.order().getTotalProductAmount()).isEqualTo(24_000L);
        assertThat(result.items()).hasSize(1);
        OrderItem item = result.items().get(0);
        assertThat(item.getTimeDealId()).isEqualTo(TIME_DEAL_ID);
        assertThat(item.getUnitPrice()).isEqualTo(12_000L);
        assertThat(item.getAppearanceType()).isEqualTo("NORMAL");
        assertThat(item.getOrigin()).isEqualTo("국내산(전남 나주)");
        assertThat(item.getHarvestDate()).isEqualTo(LocalDate.of(2026, 8, 20));
        assertThat(item.getOriginalPrice()).isEqualTo(15_000L);
        assertThat(item.getSaleUnit()).isNull();
        assertThat(item.getUnitQuantity()).isNull();
        verify(orderStatusHistoryRepository).save(any());
    }

    @Test
    @DisplayName("markStockReserved는 주문을 STOCK_RESERVED로 전이하고 이력을 남긴다")
    void 재고예약_확정() {
        Order order = withId(existingOrder("ORD-20260908-DDDDDDDD"));
        UUID orderId = order.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        Order result = orderService.markStockReserved(orderId, USER_ID);

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.STOCK_RESERVED);
        assertThat(result.getExpiresAt()).isNotNull();
        verify(orderRepository).saveAndFlush(order);
        verify(orderStatusHistoryRepository).save(any());
    }

    @Test
    @DisplayName("본인 주문이 아니면 상세 조회를 거부한다")
    void 타인주문_조회거부() {
        Order order = withId(existingOrder("ORD-20260908-BBBBBBBB"));
        UUID orderId = order.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        UUID otherUserId = UUID.randomUUID();
        assertThatThrownBy(() -> orderService.getOrderDetail(orderId, otherUserId, UserRole.CUSTOMER))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.ORDER_ACCESS_DENIED);
    }

    @Test
    @DisplayName("ADMIN은 본인 주문이 아니어도 조회할 수 있다")
    void 관리자_전체조회() {
        Order order = withId(existingOrder("ORD-20260908-CCCCCCCC"));
        UUID orderId = order.getId();
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderDeliveryGroupRepository.findByOrderId(orderId)).thenReturn(List.of());
        when(orderItemRepository.findByOrderId(orderId)).thenReturn(List.of());
        when(paymentQueryUseCase.getPayment(orderId)).thenReturn(Optional.empty());
        when(orderCancelRepository.findByOrderId(orderId)).thenReturn(List.of());

        OrderDetailData detail = orderService.getOrderDetail(orderId, UUID.randomUUID(), UserRole.ADMIN);

        assertThat(detail.orderId()).isEqualTo(order.getId());
        assertThat(detail.orderNo()).isEqualTo(order.getOrderNo());
        assertThat(detail.payment()).isNull();
        assertThat(detail.deliveryGroups()).isEmpty();
    }
}
