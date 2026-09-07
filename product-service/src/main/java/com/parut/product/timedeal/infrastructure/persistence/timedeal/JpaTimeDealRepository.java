package com.parut.product.timedeal.infrastructure.persistence.timedeal;

import com.parut.product.timedeal.domain.timedeal.TimeDeal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;


public interface JpaTimeDealRepository extends JpaRepository<TimeDeal, UUID> {

    Optional<TimeDeal> findByIdAndDeletedAtIsNull(UUID id);
}