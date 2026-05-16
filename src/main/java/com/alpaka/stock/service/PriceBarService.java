package com.alpaka.stock.service;

import com.alpaka.stock.api.dto.PriceDtos.PriceBarResponse;
import com.alpaka.stock.api.dto.PriceDtos.PriceBarUpsertRequest;
import com.alpaka.stock.domain.Instrument;
import com.alpaka.stock.domain.PriceBar;
import com.alpaka.stock.repository.PriceBarRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PriceBarService {
    private final PriceBarRepository priceBarRepository;
    private final InstrumentService instrumentService;

    public PriceBarService(PriceBarRepository priceBarRepository, InstrumentService instrumentService) {
        this.priceBarRepository = priceBarRepository;
        this.instrumentService = instrumentService;
    }

    @Transactional
    public PriceBarResponse upsert(PriceBarUpsertRequest request) {
        Instrument instrument = instrumentService.getBySymbol(request.symbol());
        PriceBar priceBar = priceBarRepository
            .findByInstrumentAndTradeDateAndProviderIgnoreCase(
                instrument,
                request.tradeDate(),
                request.provider()
            )
            .orElseGet(() -> new PriceBar(
                instrument,
                request.tradeDate(),
                request.openPrice(),
                request.highPrice(),
                request.lowPrice(),
                request.closePrice(),
                request.volume(),
                request.provider(),
                request.sourceKey()
            ));

        priceBar.updatePrices(
            request.openPrice(),
            request.highPrice(),
            request.lowPrice(),
            request.closePrice(),
            request.volume(),
            request.sourceKey()
        );
        return toResponse(priceBarRepository.save(priceBar));
    }

    @Transactional(readOnly = true)
    public List<PriceBarResponse> list(String symbol) {
        Instrument instrument = instrumentService.getBySymbol(symbol);
        return priceBarRepository.findTop180ByInstrumentOrderByTradeDateDesc(instrument)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public PriceBarResponse toResponse(PriceBar priceBar) {
        return new PriceBarResponse(
            priceBar.getId(),
            priceBar.getInstrument().getSymbol(),
            priceBar.getTradeDate(),
            priceBar.getOpenPrice(),
            priceBar.getHighPrice(),
            priceBar.getLowPrice(),
            priceBar.getClosePrice(),
            priceBar.getVolume(),
            priceBar.getProvider(),
            priceBar.getSourceKey()
        );
    }
}
