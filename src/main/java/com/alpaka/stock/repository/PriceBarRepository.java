package com.alpaka.stock.repository;

import com.alpaka.stock.domain.Instrument;
import com.alpaka.stock.domain.PriceBar;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceBarRepository extends JpaRepository<PriceBar, UUID> {
    List<PriceBar> findTop180ByInstrumentOrderByTradeDateDesc(Instrument instrument);

    List<PriceBar> findTop180ByInstrumentOrderByTradeDateAsc(Instrument instrument);

    List<PriceBar> findTop2ByInstrumentOrderByTradeDateDesc(Instrument instrument);

    Optional<PriceBar> findByInstrumentAndTradeDateAndProviderIgnoreCase(
        Instrument instrument,
        LocalDate tradeDate,
        String provider
    );

    long countByInstrument(Instrument instrument);
}
