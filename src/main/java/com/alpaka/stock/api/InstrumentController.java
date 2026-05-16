package com.alpaka.stock.api;

import com.alpaka.stock.api.dto.InstrumentDtos.InstrumentResponse;
import com.alpaka.stock.api.dto.InstrumentDtos.InstrumentSearchResponse;
import com.alpaka.stock.api.dto.InstrumentDtos.InstrumentUpsertRequest;
import com.alpaka.stock.service.InstrumentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/instruments", "/api/v1/instruments"})
public class InstrumentController {
    private final InstrumentService instrumentService;

    public InstrumentController(InstrumentService instrumentService) {
        this.instrumentService = instrumentService;
    }

    @GetMapping("/search")
    public InstrumentSearchResponse search(@RequestParam("q") String query) {
        return new InstrumentSearchResponse(instrumentService.search(query));
    }

    @PostMapping
    public InstrumentResponse upsert(@Valid @RequestBody InstrumentUpsertRequest request) {
        return instrumentService.upsert(request);
    }
}
