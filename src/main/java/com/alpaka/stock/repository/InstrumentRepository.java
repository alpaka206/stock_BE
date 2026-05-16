package com.alpaka.stock.repository;

import com.alpaka.stock.domain.Instrument;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InstrumentRepository extends JpaRepository<Instrument, UUID> {
    Optional<Instrument> findBySymbolIgnoreCaseAndExchangeIgnoreCase(String symbol, String exchange);

    Optional<Instrument> findFirstBySymbolIgnoreCase(String symbol);

    List<Instrument> findTop20BySymbolContainingIgnoreCaseOrNameContainingIgnoreCaseOrderBySymbolAsc(
        String symbol,
        String name
    );

    List<Instrument> findTop100ByActiveTrueOrderBySymbolAsc();

    List<Instrument> findTop12BySectorIgnoreCaseAndActiveTrueOrderBySymbolAsc(String sector);
}
