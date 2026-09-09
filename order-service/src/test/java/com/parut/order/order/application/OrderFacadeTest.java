package com.parut.order.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.parut.order.global.exception.BusinessException;
import com.parut.order.global.exception.ErrorCode;
import com.parut.order.order.application.dto.CreateOrderCommand;
import com.parut.order.order.application.dto.CreateTimeDealOrderCommand;
import com.parut.order.order.application.dto.CreatedOrder;
import com.parut.order.order.application.port.out.ProductClient;
import com.parut.order.order.application.port.out.TimeDealClient;
import com.parut.order.order.application.port.out.dto.ProductOrderInfo;
import com.parut.order.order.application.port.out.dto.TimeDealInfo;
import com.parut.order.order.domain.Order;
import com.parut.order.order.domain.OrderItem;
import com.parut.order.order.domain.OrderStatus;
import com.parut.order.order.domain.OrderType;

@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

    private static final UUID USER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b1");
    private static final UUID PRODUCT_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b2");
    private static final UUID SELLER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b3");
    private static final UUID TIME_DEAL_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b4");
    private static final String IDEMPOTENCY_KEY = "idem-key-0001";

    @Mock
    private OrderService orderService;

    @Mock
    private ProductClient productClient;

    @Mock
    private TimeDealClient timeDealClient;

    @InjectMocks
    private OrderFacade orderFacade;

    private CreateOrderCommand createCommand(int quantity) {
        return new CreateOrderCommand(
                USER_ID, IDEMPOTENCY_KEY, PRODUCT_ID, quantity,
                "홍길동", "01012345678", "06234", "서울특별시 강남구 테헤란로 123", "5층 501호", null
        );
    }

    private ProductOrderInfo purchasableProductInfo() {
        return new ProductOrderInfo(
                PRODUCT_ID, SELLER_ID, "신고배 5kg 특품", "NORMAL", "국내산(전남 나주)",
                LocalDate.of(2026, 8, 20), "KG", BigDecimal.valueOf(5), 15_000L, true
        );
    }

    private Order existingOrder(String orderNo) {
        Order order = Order.create(
                orderNo, USER_ID, OrderType.NORMAL,
                "홍길동", "01012345678", "06234", "서울특별시 강남구 테헤란로 123", null, null,
                30_000L, 3_000L, IDEMPOTENCY_KEY
        );
        ReflectionTestUtils.setField(order, "id", UUID.randomUUID());
        return order;
    }

    private OrderItem existingItem(Order order) {
        OrderItem item = OrderItem.create(
                order.getId(), UUID.randomUUID(), PRODUCT_ID, null, "신고배 5kg 특품",
                "NORMAL", "국내산(전남 나주)", LocalDate.of(2026, 8, 20), "KG", BigDecimal.valueOf(5),
                15_000L, 15_000L, 2
        );
        ReflectionTestUtils.setField(item, "id", UUID.randomUUID());
        return item;
    }

    private CreateTimeDealOrderCommand createTimeDealCommand(int quantity) {
        return new CreateTimeDealOrderCommand(
                USER_ID, IDEMPOTENCY_KEY, TIME_DEAL_ID, PRODUCT_ID, quantity,
                "홍길동", "01012345678", "06234", "서울특별시 강남구 테헤란로 123", "5층 501호", null
        );
    }

    private TimeDealInfo timeDealInfo() {
        return new TimeDealInfo(TIME_DEAL_ID, PRODUCT_ID, SELLER_ID, "신고배 5kg 특품(타임딜)", 12_000L);
    }

    private OrderItem existingTimeDealItem(Order order) {
        OrderItem item = OrderItem.create(
                order.getId(), UUID.randomUUID(), PRODUCT_ID, TIME_DEAL_ID, "신고배 5kg 특품(타임딜)",
                null, null, null, null, null, null, 12_000L, 2
        );
        ReflectionTestUtils.setField(item, "id", UUID.randomUUID());
        return item;
    }

    @Test
    @DisplayName("멱등키로 조회된 기존 주문이 있으면 새로 생성하지 않고 그대로 반환한다")
    void 멱등키_기존주문_반환() {
        Order existing = existingOrder("ORD-20260908-AAAAAAAA");
        when(orderService.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.of(existing));

        Order result = orderFacade.createOrder(createCommand(1));

        assertThat(result).isSameAs(existing);
        verifyNoInteractions(productClient);
        verify(orderService, never()).saveNewOrder(any(), any());
    }

    @Test
    @DisplayName("재고 예약까지 성공하면 STOCK_RESERVED 주문을 반환한다")
    void 주문생성_성공() {
        Order created = existingOrder("ORD-20260908-EEEEEEEE");
        OrderItem item = existingItem(created);
        Order reserved = existingOrder("ORD-20260908-EEEEEEEE");
        ReflectionTestUtils.setField(reserved, "id", created.getId());
        reserved.markStockReserved(java.time.Instant.now().plusSeconds(3600));

        when(orderService.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(productClient.getOrderInfo(PRODUCT_ID)).thenReturn(purchasableProductInfo());
        when(orderService.saveNewOrder(any(), any())).thenReturn(new CreatedOrder(created, item));
        when(orderService.markStockReserved(created.getId(), USER_ID)).thenReturn(reserved);

        Order result = orderFacade.createOrder(createCommand(2));

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.STOCK_RESERVED);
        verify(productClient).reserveStock(eq(PRODUCT_ID), eq(created.getId()), eq(item.getId()), eq(2));
        verify(orderService, never()).deleteFailedOrder(any());
    }

    @Test
    @DisplayName("재고 예약이 실패하면 저장된 주문을 삭제하고 예외를 전파한다")
    void 재고부족_주문삭제() {
        Order created = existingOrder("ORD-20260908-FFFFFFFF");
        OrderItem item = existingItem(created);

        when(orderService.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(productClient.getOrderInfo(PRODUCT_ID)).thenReturn(purchasableProductInfo());
        when(orderService.saveNewOrder(any(), any())).thenReturn(new CreatedOrder(created, item));
        doThrow(new BusinessException(ErrorCode.STOCK_SHORTAGE))
                .when(productClient).reserveStock(any(), any(), any(), anyInt());

        assertThatThrownBy(() -> orderFacade.createOrder(createCommand(1)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.STOCK_SHORTAGE);

        verify(orderService).deleteFailedOrder(created.getId());
        verify(orderService, never()).markStockReserved(any(), any());
    }

    @Test
    @DisplayName("판매 불가 상품이면 PRODUCT_UNAVAILABLE 예외를 던지고 주문을 저장하지 않는다")
    void 판매불가_상품() {
        when(orderService.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(productClient.getOrderInfo(PRODUCT_ID)).thenReturn(new ProductOrderInfo(
                PRODUCT_ID, SELLER_ID, "품절 상품", "NORMAL", "국내산",
                LocalDate.of(2026, 8, 20), "KG", BigDecimal.valueOf(5), 15_000L, false
        ));

        assertThatThrownBy(() -> orderFacade.createOrder(createCommand(1)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.PRODUCT_UNAVAILABLE);

        verify(orderService, never()).saveNewOrder(any(), any());
    }

    @Test
    @DisplayName("타임딜 재고 예약까지 성공하면 STOCK_RESERVED 주문을 반환한다")
    void 타임딜주문생성_성공() {
        Order created = existingOrder("ORD-20260908-GGGGGGGG");
        OrderItem item = existingTimeDealItem(created);
        Order reserved = existingOrder("ORD-20260908-GGGGGGGG");
        ReflectionTestUtils.setField(reserved, "id", created.getId());
        reserved.markStockReserved(java.time.Instant.now().plusSeconds(3600));

        when(orderService.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(timeDealClient.getOrderInfo(TIME_DEAL_ID)).thenReturn(timeDealInfo());
        when(orderService.saveNewTimeDealOrder(any(), any())).thenReturn(new CreatedOrder(created, item));
        when(orderService.markStockReserved(created.getId(), USER_ID)).thenReturn(reserved);

        Order result = orderFacade.createTimeDealOrder(createTimeDealCommand(2));

        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.STOCK_RESERVED);
        verify(timeDealClient).reserveStock(TIME_DEAL_ID, created.getId(), USER_ID, 2);
        verifyNoInteractions(productClient);
        verify(orderService, never()).deleteFailedOrder(any());
    }

    @Test
    @DisplayName("타임딜 재고 예약이 실패하면 저장된 주문을 삭제하고 예외를 전파한다")
    void 타임딜재고부족_주문삭제() {
        Order created = existingOrder("ORD-20260908-HHHHHHHH");
        OrderItem item = existingTimeDealItem(created);

        when(orderService.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(timeDealClient.getOrderInfo(TIME_DEAL_ID)).thenReturn(timeDealInfo());
        when(orderService.saveNewTimeDealOrder(any(), any())).thenReturn(new CreatedOrder(created, item));
        doThrow(new BusinessException(ErrorCode.STOCK_SHORTAGE))
                .when(timeDealClient).reserveStock(any(), any(), any(), anyInt());

        assertThatThrownBy(() -> orderFacade.createTimeDealOrder(createTimeDealCommand(1)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.STOCK_SHORTAGE);

        verify(orderService).deleteFailedOrder(created.getId());
        verify(orderService, never()).markStockReserved(any(), any());
    }
}
