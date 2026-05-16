package com.alpaka.stock.repository;

import com.alpaka.stock.domain.SubscriptionPlan;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, UUID> {
    Optional<SubscriptionPlan> findByCodeIgnoreCase(String code);

    List<SubscriptionPlan> findByActiveTrueOrderByMonthlyPriceAsc();
}
