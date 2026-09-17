package com.parut.order.delivery.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

import com.parut.order.delivery.application.DeliveryService;
import com.parut.order.delivery.domain.Delivery;
import com.parut.order.delivery.domain.DeliveryStatus;
import com.parut.order.delivery.infrastructure.persistence.DeliveryRepository;
import com.parut.order.global.config.JpaAuditingConfig;
import com.parut.order.order.application.OrderDeliveryGroupStatusService;
import com.parut.order.order.application.port.in.OrderDeliveryGroupQueryUseCase;
import com.parut.order.order.domain.DeliveryGroupStatus;
import com.parut.order.order.domain.OrderDeliveryGroup;
import com.parut.order.order.infrastructure.persistence.OrderDeliveryGroupRepository;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "delivery.completion.delay-seconds=60"
})
@ActiveProfiles("test")
@Import({DeliveryService.class, OrderDeliveryGroupStatusService.class,
        DeliveryCompletionScheduler.class, JpaAuditingConfig.class})
// 배치가 연 트랜잭션의 커밋과 롤백을 확인하므로 테스트 전체를 트랜잭션으로 감싸지 않는다.
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DeliveryCompletionTransactionTest {

    private static final Instant SHIPPED_AT = Instant.parse("2026-09-01T00:00:00Z");

    @Autowired
    private DeliveryCompletionScheduler scheduler;

    @Autowired
    private DeliveryRepository deliveryRepository;

    @Autowired
    private OrderDeliveryGroupRepository groupRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private OrderDeliveryGroupQueryUseCase groupQueryUseCase;

    @Test
    @DisplayName("중간 배송의 변경은 롤백하고 앞뒤 배송과 배송그룹은 커밋한다")
    void 건별_롤백과_커밋() {
        List<Delivery> deliveries = createDeliveriesWithInvalidMiddleGroup();

        scheduler.runDeliveryCompletion();

        Delivery failedDelivery = deliveryRepository.findById(deliveries.get(1).getId()).orElseThrow();
        OrderDeliveryGroup failedGroup = groupRepository.findById(failedDelivery.getDeliveryGroupId()).orElseThrow();
        assertThat(failedDelivery.getStatus()).isEqualTo(DeliveryStatus.SHIPPED);
        assertThat(failedDelivery.getDeliveredAt()).isNull();
        assertThat(failedGroup.getGroupStatus()).isEqualTo(DeliveryGroupStatus.PREPARING);

        for (Delivery expectedDelivery : List.of(deliveries.getFirst(), deliveries.getLast())) {
            Delivery delivery = deliveryRepository.findById(expectedDelivery.getId()).orElseThrow();
            OrderDeliveryGroup group = groupRepository.findById(delivery.getDeliveryGroupId()).orElseThrow();
            assertThat(delivery.getStatus()).isEqualTo(DeliveryStatus.DELIVERED);
            assertThat(delivery.getDeliveredAt()).isNotNull();
            assertThat(group.getGroupStatus()).isEqualTo(DeliveryGroupStatus.DELIVERED);
        }
    }

    private List<Delivery> createDeliveriesWithInvalidMiddleGroup() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        return transaction.execute(status -> {
            List<Delivery> result = new ArrayList<>();
            // 성공, 실패, 성공 순서로 처리해 실패 전 커밋과 실패 후 계속 처리를 함께 검증한다.
            for (int i = 0; i < 3; i++) {
                OrderDeliveryGroup group = OrderDeliveryGroup.create(
                        UUID.randomUUID(), UUID.randomUUID(), 1000L, 0L);
                group.markPreparing();
                groupRepository.save(group);
                Delivery delivery = Delivery.create(group.getId());
                delivery.ship("1234567890", SHIPPED_AT);
                result.add(deliveryRepository.save(delivery));
            }
            // 생성된 UUID는 무작위이므로 조회 쿼리의 ID 순서에 맞춰 가운데 실패 건을 정한다.
            result.sort(Comparator.comparing(delivery -> delivery.getId().toString()));
            // 가운데 배송그룹은 PREPARING으로 남겨 실제 상태 전이에서 실패시킨다.
            for (int index : List.of(0, 2)) {
                groupRepository.findById(result.get(index).getDeliveryGroupId()).orElseThrow().markShipped();
            }
            return result;
        });
    }
}
