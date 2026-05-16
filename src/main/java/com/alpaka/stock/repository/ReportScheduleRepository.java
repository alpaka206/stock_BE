package com.alpaka.stock.repository;

import com.alpaka.stock.domain.ReportSchedule;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, UUID> {
    List<ReportSchedule> findTop50ByUserIdOrderByCreatedAtDesc(String userId);
}
