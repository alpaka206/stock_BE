package com.alpaka.stock.api;

import com.alpaka.stock.api.dto.PriceDtos.PriceBarListResponse;
import com.alpaka.stock.api.dto.PriceDtos.PriceBarResponse;
import com.alpaka.stock.api.dto.PriceDtos.PriceBarUpsertRequest;
import com.alpaka.stock.service.PriceBarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/prices", "/api/v1/prices"})
@Tag(name = "Prices", description = "종목별 가격 바 저장 API")
public class PriceBarController {
    private final PriceBarService priceBarService;

    public PriceBarController(PriceBarService priceBarService) {
        this.priceBarService = priceBarService;
    }

    @GetMapping("/bars")
    @Operation(summary = "가격 바 조회", description = "종목별 저장 가격 바를 최신순으로 조회합니다.")
    public PriceBarListResponse list(@RequestParam String symbol) {
        return new PriceBarListResponse(priceBarService.list(symbol));
    }

    @PostMapping("/bars")
    @Operation(summary = "가격 바 저장", description = "instrument/date/provider 단위로 일별 가격 바를 upsert합니다.")
    public PriceBarResponse upsert(@Valid @org.springframework.web.bind.annotation.RequestBody PriceBarUpsertRequest request) {
        return priceBarService.upsert(request);
    }
}
