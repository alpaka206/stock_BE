package com.alpaka.stock.service;

import com.alpaka.stock.api.dto.InstrumentDtos.InstrumentResponse;
import com.alpaka.stock.api.dto.InstrumentDtos.InstrumentUpsertRequest;
import com.alpaka.stock.domain.Instrument;
import com.alpaka.stock.repository.InstrumentRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstrumentService {
    private final InstrumentRepository instrumentRepository;

    public InstrumentService(InstrumentRepository instrumentRepository) {
        this.instrumentRepository = instrumentRepository;
    }

    @Transactional
    public InstrumentResponse upsert(InstrumentUpsertRequest request) {
        Instrument instrument = instrumentRepository
            .findBySymbolIgnoreCaseAndExchangeIgnoreCase(request.symbol(), request.exchange())
            .orElseGet(() -> new Instrument(
                request.symbol(),
                request.name(),
                request.market(),
                request.exchange(),
                request.securityCode(),
                request.sector(),
                request.currency()
            ));

        instrument.updateProfile(request.name(), request.sector(), request.currency(), request.active());
        return toResponse(instrumentRepository.save(instrument));
    }

    @Transactional(readOnly = true)
    public Instrument getBySymbol(String symbol) {
        return instrumentRepository.findFirstBySymbolIgnoreCase(symbol)
            .orElseThrow(() -> new EntityNotFoundException("종목을 찾을 수 없습니다: " + symbol));
    }

    @Transactional(readOnly = true)
    public List<InstrumentResponse> search(String query) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.isBlank()) {
            return List.of();
        }

        return instrumentRepository
            .findTop20BySymbolContainingIgnoreCaseOrNameContainingIgnoreCaseOrderBySymbolAsc(
                normalized,
                normalized
            )
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public InstrumentResponse toResponse(Instrument instrument) {
        return new InstrumentResponse(
            instrument.getId(),
            instrument.getSymbol(),
            instrument.getName(),
            instrument.getMarket(),
            instrument.getExchange(),
            instrument.getSecurityCode(),
            instrument.getSector(),
            instrument.getCurrency(),
            instrument.isActive()
        );
    }
}
