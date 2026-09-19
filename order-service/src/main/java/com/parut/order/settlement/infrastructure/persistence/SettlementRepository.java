package com.parut.order.settlement.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.parut.order.settlement.domain.Settlement;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {
}
