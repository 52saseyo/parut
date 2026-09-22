package com.parut.product.global.outbox.infrastructure.persistence;

import com.parut.product.global.outbox.domain.OutboxPublishStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaOutboxEventRepository extends JpaRepository<JpaOutboxEvent, UUID> {

    List<JpaOutboxEvent> findTop100ByPublishStatusOrderByCreatedAtAsc(OutboxPublishStatus publishStatus);
}
