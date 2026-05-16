package com.alpaka.stock.api;

import com.alpaka.stock.service.ContractViewService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"", "/api/v1"})
public class WorkspaceController {
    private final ContractViewService contractViewService;

    public WorkspaceController(ContractViewService contractViewService) {
        this.contractViewService = contractViewService;
    }

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        return contractViewService.overview();
    }

    @GetMapping("/radar")
    public Map<String, Object> radar() {
        return contractViewService.radar();
    }

    @GetMapping("/stocks/{symbol}")
    public Map<String, Object> stock(@PathVariable String symbol) {
        return contractViewService.stock(symbol);
    }

    @GetMapping("/history")
    public Map<String, Object> history(
        @RequestParam(required = false) String symbol,
        @RequestParam(required = false) String range
    ) {
        return contractViewService.history(symbol, range);
    }

    @GetMapping("/news")
    public Map<String, Object> news() {
        return contractViewService.news();
    }

    @GetMapping("/calendar")
    public Map<String, Object> calendar() {
        return contractViewService.calendar();
    }
}
