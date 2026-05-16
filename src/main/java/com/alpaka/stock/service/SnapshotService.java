package com.alpaka.stock.service;

import com.alpaka.stock.api.dto.SnapshotDtos.SnapshotCreateRequest;
import com.alpaka.stock.api.dto.SnapshotDtos.SnapshotResponse;
import com.alpaka.stock.domain.Instrument;
import com.alpaka.stock.domain.ResearchSnapshot;
import com.alpaka.stock.repository.ResearchSnapshotRepository;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SnapshotService {
    private final ResearchSnapshotRepository researchSnapshotRepository;
    private final InstrumentService instrumentService;

    public SnapshotService(
        ResearchSnapshotRepository researchSnapshotRepository,
        InstrumentService instrumentService
    ) {
        this.researchSnapshotRepository = researchSnapshotRepository;
        this.instrumentService = instrumentService;
    }

    @Transactional
    public SnapshotResponse create(SnapshotCreateRequest request) {
        Instrument instrument = instrumentService.getBySymbol(request.symbol());
        String activeRuleLabels = request.activeRuleLabels() == null
            ? ""
            : String.join("\n", request.activeRuleLabels());
        ResearchSnapshot snapshot = new ResearchSnapshot(
            request.userId(),
            instrument,
            request.note(),
            request.stance(),
            request.conviction(),
            request.thesis(),
            request.price(),
            request.changePercent(),
            request.score(),
            request.selectedEventTitle(),
            request.selectedEventDate(),
            activeRuleLabels,
            request.presetName()
        );
        return toResponse(researchSnapshotRepository.save(snapshot));
    }

    @Transactional(readOnly = true)
    public List<SnapshotResponse> list(String symbol) {
        if (symbol != null && !symbol.isBlank()) {
            Instrument instrument = instrumentService.getBySymbol(symbol);
            return researchSnapshotRepository.findTop120ByInstrumentOrderByCreatedAtDesc(instrument)
                .stream()
                .map(this::toResponse)
                .toList();
        }
        return researchSnapshotRepository.findTop120ByOrderByCreatedAtDesc()
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @Transactional
    public boolean delete(UUID id) {
        if (!researchSnapshotRepository.existsById(id)) {
            return false;
        }
        researchSnapshotRepository.deleteById(id);
        return true;
    }

    private SnapshotResponse toResponse(ResearchSnapshot snapshot) {
        Instrument instrument = snapshot.getInstrument();
        return new SnapshotResponse(
            snapshot.getId(),
            snapshot.getUserId(),
            instrument.getSymbol(),
            instrument.getName(),
            instrument.getExchange(),
            instrument.getSecurityCode(),
            instrument.getSector(),
            snapshot.getNote(),
            snapshot.getStance(),
            snapshot.getConviction(),
            snapshot.getPrice(),
            snapshot.getChangePercent(),
            snapshot.getScore(),
            snapshot.getThesis(),
            snapshot.getSelectedEventTitle(),
            snapshot.getSelectedEventDate(),
            splitLines(snapshot.getActiveRuleLabels()),
            snapshot.getPresetName(),
            snapshot.getCreatedAt()
        );
    }

    private List<String> splitLines(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\\R"))
            .filter(item -> !item.isBlank())
            .toList();
    }
}
