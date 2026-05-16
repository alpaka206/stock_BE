package com.alpaka.stock.repository;

import com.alpaka.stock.domain.SourceKind;
import com.alpaka.stock.domain.SourceMaterial;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SourceMaterialRepository extends JpaRepository<SourceMaterial, UUID> {
    Optional<SourceMaterial> findByProviderIgnoreCaseAndSourceKey(String provider, String sourceKey);

    List<SourceMaterial> findTop20ByOrderByPublishedAtDesc();

    List<SourceMaterial> findTop20ByKindOrderByPublishedAtDesc(SourceKind kind);

    List<SourceMaterial> findTop50ByOrderByPublishedAtDesc();

    List<SourceMaterial> findTop20ByInstrumentOrderByPublishedAtDesc(
        com.alpaka.stock.domain.Instrument instrument
    );

    List<SourceMaterial> findTop20ByInstrumentAndKindOrderByPublishedAtDesc(
        com.alpaka.stock.domain.Instrument instrument,
        SourceKind kind
    );

    long countByKind(SourceKind kind);
}
