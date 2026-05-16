package com.alpaka.stock.api;

import com.alpaka.stock.api.dto.MaterialDtos.MaterialIngestRequest;
import com.alpaka.stock.api.dto.MaterialDtos.MaterialListResponse;
import com.alpaka.stock.api.dto.MaterialDtos.MaterialResponse;
import com.alpaka.stock.domain.SourceKind;
import com.alpaka.stock.service.MaterialService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/materials", "/api/v1/materials"})
public class MaterialController {
    private final MaterialService materialService;

    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @GetMapping
    public MaterialListResponse latest(@RequestParam(value = "kind", required = false) SourceKind kind) {
        return new MaterialListResponse(materialService.latest(kind));
    }

    @PostMapping
    public MaterialResponse ingest(@Valid @RequestBody MaterialIngestRequest request) {
        return materialService.ingest(request);
    }
}
