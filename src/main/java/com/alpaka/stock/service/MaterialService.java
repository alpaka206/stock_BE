package com.alpaka.stock.service;

import com.alpaka.stock.api.dto.MaterialDtos.MaterialIngestRequest;
import com.alpaka.stock.api.dto.MaterialDtos.MaterialResponse;
import com.alpaka.stock.domain.Instrument;
import com.alpaka.stock.domain.SourceKind;
import com.alpaka.stock.domain.SourceMaterial;
import com.alpaka.stock.repository.InstrumentRepository;
import com.alpaka.stock.repository.SourceMaterialRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaterialService {
    private final SourceMaterialRepository sourceMaterialRepository;
    private final InstrumentRepository instrumentRepository;

    public MaterialService(
        SourceMaterialRepository sourceMaterialRepository,
        InstrumentRepository instrumentRepository
    ) {
        this.sourceMaterialRepository = sourceMaterialRepository;
        this.instrumentRepository = instrumentRepository;
    }

    @Transactional
    public MaterialResponse ingest(MaterialIngestRequest request) {
        return sourceMaterialRepository
            .findByProviderIgnoreCaseAndSourceKey(request.provider(), request.sourceKey())
            .map(this::toResponse)
            .orElseGet(() -> createMaterial(request));
    }

    @Transactional(readOnly = true)
    public List<MaterialResponse> latest(SourceKind kind) {
        List<SourceMaterial> materials = kind == null
            ? sourceMaterialRepository.findTop20ByOrderByPublishedAtDesc()
            : sourceMaterialRepository.findTop20ByKindOrderByPublishedAtDesc(kind);
        return materials.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public long countByKind(SourceKind kind) {
        return sourceMaterialRepository.countByKind(kind);
    }

    private MaterialResponse createMaterial(MaterialIngestRequest request) {
        Instrument instrument = request.symbol() == null || request.symbol().isBlank()
            ? null
            : instrumentRepository.findFirstBySymbolIgnoreCase(request.symbol()).orElse(null);
        String rawPayload = request.rawPayload() == null ? "" : request.rawPayload();
        String checksum = sha256(request.provider() + ":" + request.sourceKey() + ":" + rawPayload);
        SourceMaterial material = new SourceMaterial(
            instrument,
            request.kind(),
            request.provider(),
            request.publisher(),
            request.title(),
            request.summary(),
            request.sourceUrl(),
            request.sourceKey(),
            request.language(),
            request.publishedAt(),
            rawPayload,
            checksum
        );
        return toResponse(sourceMaterialRepository.save(material));
    }

    public MaterialResponse toResponse(SourceMaterial material) {
        String symbol = material.getInstrument() == null ? "" : material.getInstrument().getSymbol();
        return new MaterialResponse(
            material.getId(),
            symbol,
            material.getKind(),
            material.getProvider(),
            material.getPublisher(),
            material.getTitle(),
            material.getSummary(),
            material.getSourceUrl(),
            material.getSourceKey(),
            material.getLanguage(),
            material.getPublishedAt()
        );
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exc) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", exc);
        }
    }
}
