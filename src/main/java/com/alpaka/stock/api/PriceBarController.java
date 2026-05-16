package com.alpaka.stock.api;

import com.alpaka.stock.api.dto.PriceDtos.PriceBarListResponse;
import com.alpaka.stock.api.dto.PriceDtos.PriceBarResponse;
import com.alpaka.stock.api.dto.PriceDtos.PriceBarUpsertRequest;
import com.alpaka.stock.service.PriceBarService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/prices", "/api/v1/prices"})
public class PriceBarController {
    private final PriceBarService priceBarService;

    public PriceBarController(PriceBarService priceBarService) {
        this.priceBarService = priceBarService;
    }

    @GetMapping("/bars")
    public PriceBarListResponse list(@RequestParam String symbol) {
        return new PriceBarListResponse(priceBarService.list(symbol));
    }

    @PostMapping("/bars")
    public PriceBarResponse upsert(@Valid @org.springframework.web.bind.annotation.RequestBody PriceBarUpsertRequest request) {
        return priceBarService.upsert(request);
    }
}
