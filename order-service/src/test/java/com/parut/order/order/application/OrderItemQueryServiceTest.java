package com.parut.order.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.parut.order.order.application.port.in.dto.OrderItemView;
import com.parut.order.order.domain.Order;
import com.parut.order.order.domain.OrderDeliveryGroup;
import com.parut.order.order.domain.OrderItem;
import com.parut.order.order.domain.OrderType;
import com.parut.order.order.infrastructure.persistence.OrderDeliveryGroupRepository;
import com.parut.order.order.infrastructure.persistence.OrderItemRepository;
import com.parut.order.order.infrastructure.persistence.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderItemQueryServiceTest {

    private static final UUID USER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b1");
    private static final UUID PRODUCT_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b2");
    private static final UUID SELLER_ID = UUID.fromString("01991a36-dfe8-78b4-aeb5-ec869d15a6b3");

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderDeliveryGroupRepository orderDeliveryGroupRepository;

    @InjectMocks
    private OrderItemQueryService orderItemQueryService;

    @Test
    @DisplayName("주문상품 ID로 조회하면 주문·배송그룹 정보를 합쳐 반환한다")
    void 주문상품_조회() {
        Order order = withId(order());
        OrderDeliveryGroup group = deliveryGroup(order);
        OrderItem item = orderItem(order, group);

        when(orderItemRepository.findAllById(anyList())).thenReturn(List.of(item));
        when(orderRepository.findAllById(anyList())).thenReturn(List.of(order));
        when(orderDeliveryGroupRepository.findAllById(anyList())).thenReturn(List.of(group));

        List<OrderItemView> result = orderItemQueryService.getOrderItems(List.of(item.getId()));

        assertThat(result).hasSize(1);
        OrderItemView view = result.get(0);
        assertThat(view.orderItemId()).isEqualTo(item.getId());
        assertThat(view.orderId()).isEqualTo(order.getId());
        assertThat(view.buyerId()).isEqualTo(order.getUserId());
        assertThat(view.sellerId()).isEqualTo(group.getSellerId());
        assertThat(view.groupStatus()).isEqualTo(group.getGroupStatus());
        assertThat(view.unitPrice()).isEqualTo(item.getUnitPrice());
        assertThat(view.quantity()).isEqualTo(item.getQuantity());
    }

    @Test
    @DisplayName("존재하지 않는 ID는 결과에서 제외된다")
    void 존재하지_않는_ID_제외() {
        when(orderItemRepository.findAllById(anyList())).thenReturn(List.of());
        when(orderRepository.findAllById(anyList())).thenReturn(List.of());
        when(orderDeliveryGroupRepository.findAllById(anyList())).thenReturn(List.of());

        List<OrderItemView> result = orderItemQueryService.getOrderItems(List.of(UUID.randomUUID()));

        assertThat(result).isEmpty();
    }

    private Order order() {
        return Order.create(
                "ORD-20260910-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                USER_ID,
                OrderType.NORMAL,
                "홍길동",
                "01012345678",
                "06234",
                "서울특별시 강남구 테헤란로 123",
                null,
                null,
                15_000L,
                3_000L,
                "query-idem-key-" + UUID.randomUUID()
        );
    }

    private OrderDeliveryGroup deliveryGroup(Order order) {
        return withId(OrderDeliveryGroup.create(order.getId(), SELLER_ID, 15_000L, 3_000L));
    }

    private OrderItem orderItem(Order order, OrderDeliveryGroup group) {
        return withId(OrderItem.create(
                order.getId(),
                group.getId(),
                PRODUCT_ID,
                null,
                "신고배 5kg 특품",
                "NORMAL",
                "국내산(전남 나주)",
                LocalDate.of(2026, 8, 20),
                "KG",
                BigDecimal.valueOf(5),
                15_000L,
                15_000L,
                1
        ));
    }

    private <T> T withId(T entity) {
        ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
        return entity;
    }
}
