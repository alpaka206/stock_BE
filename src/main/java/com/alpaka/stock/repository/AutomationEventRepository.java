package com.alpaka.stock.repository;

import com.alpaka.stock.domain.AutomationEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutomationEventRepository extends JpaRepository<AutomationEvent, UUID> {
    List<AutomationEvent> findTop50BySourceOrderByCreatedAtDesc(String source);
}
