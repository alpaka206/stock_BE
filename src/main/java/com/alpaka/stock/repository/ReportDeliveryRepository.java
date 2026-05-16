package com.alpaka.stock.repository;

import com.alpaka.stock.domain.ReportDelivery;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportDeliveryRepository extends JpaRepository<ReportDelivery, UUID> {
    List<ReportDelivery> findTop50ByUserIdOrderByGeneratedAtDesc(String userId);
}
