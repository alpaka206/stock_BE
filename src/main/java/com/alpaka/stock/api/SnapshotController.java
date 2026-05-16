package com.alpaka.stock.api;

import com.alpaka.stock.api.dto.SnapshotDtos.SnapshotCreateRequest;
import com.alpaka.stock.api.dto.SnapshotDtos.SnapshotDeleteResponse;
import com.alpaka.stock.api.dto.SnapshotDtos.SnapshotListResponse;
import com.alpaka.stock.api.dto.SnapshotDtos.SnapshotMutationResponse;
import com.alpaka.stock.service.SnapshotService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/snapshots", "/api/v1/snapshots"})
public class SnapshotController {
    private final SnapshotService snapshotService;

    public SnapshotController(SnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @GetMapping
    public SnapshotListResponse list(@RequestParam(value = "symbol", required = false) String symbol) {
        return new SnapshotListResponse(snapshotService.list(symbol));
    }

    @PostMapping
    public SnapshotMutationResponse create(@Valid @RequestBody SnapshotCreateRequest request) {
        return new SnapshotMutationResponse(snapshotService.create(request));
    }

    @DeleteMapping("/{id}")
    public SnapshotDeleteResponse delete(@PathVariable UUID id) {
        return new SnapshotDeleteResponse(id, snapshotService.delete(id));
    }
}
