package com.alpaka.stock.repository;

import com.alpaka.stock.domain.Instrument;
import com.alpaka.stock.domain.PriceBar;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceBarRepository extends JpaRepository<PriceBar, UUID> {
    List<PriceBar> findTop180ByInstrumentOrderByTradeDateDesc(Instrument instrument);

    long countByInstrument(Instrument instrument);
}
