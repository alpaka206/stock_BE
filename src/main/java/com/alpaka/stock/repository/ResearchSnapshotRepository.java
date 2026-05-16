package com.alpaka.stock.repository;

import com.alpaka.stock.domain.Instrument;
import com.alpaka.stock.domain.ResearchSnapshot;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResearchSnapshotRepository extends JpaRepository<ResearchSnapshot, UUID> {
    List<ResearchSnapshot> findTop120ByOrderByCreatedAtDesc();

    List<ResearchSnapshot> findTop120ByInstrumentOrderByCreatedAtDesc(Instrument instrument);
}
