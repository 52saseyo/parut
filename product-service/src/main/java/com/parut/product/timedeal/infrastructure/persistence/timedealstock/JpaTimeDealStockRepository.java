package com.parut.product.timedeal.infrastructure.persistence.timedealstock;

import com.parut.product.timedeal.domain.timedealstock.TimeDealStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;


public interface JpaTimeDealStockRepository extends JpaRepository<TimeDealStock, UUID> {

    Optional<TimeDealStock> findByTimeDealIdAndDeletedAtIsNull(UUID timeDealId);
}