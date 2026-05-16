package com.alpaka.stock.api;

import com.alpaka.stock.api.dto.WorkspaceDtos.WorkspaceOverviewResponse;
import com.alpaka.stock.service.WorkspaceQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"", "/api/v1"})
public class WorkspaceController {
    private final WorkspaceQueryService workspaceQueryService;

    public WorkspaceController(WorkspaceQueryService workspaceQueryService) {
        this.workspaceQueryService = workspaceQueryService;
    }

    @GetMapping("/overview")
    public WorkspaceOverviewResponse overview() {
        return workspaceQueryService.overview();
    }
}
