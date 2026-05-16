package com.alpaka.stock.repository;

import com.alpaka.stock.domain.SourceKind;
import com.alpaka.stock.domain.SourceMaterial;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SourceMaterialRepository extends JpaRepository<SourceMaterial, UUID> {
    Optional<SourceMaterial> findByProviderIgnoreCaseAndSourceKey(String provider, String sourceKey);

    default List<SourceMaterial> findTop20ByOrderByPublishedAtDesc() {
        return findRecent(PageRequest.of(0, 20));
    }

    @Query("select material from SourceMaterial material order by case when material.publishedAt is null then 1 else 0 end, material.publishedAt desc, material.fetchedAt desc")
    List<SourceMaterial> findRecent(Pageable pageable);

    default List<SourceMaterial> findTop20ByKindOrderByPublishedAtDesc(SourceKind kind) {
        return findRecentByKind(kind, PageRequest.of(0, 20));
    }

    @Query("select material from SourceMaterial material where material.kind = :kind order by case when material.publishedAt is null then 1 else 0 end, material.publishedAt desc, material.fetchedAt desc")
    List<SourceMaterial> findRecentByKind(@Param("kind") SourceKind kind, Pageable pageable);

    default List<SourceMaterial> findTop50ByOrderByPublishedAtDesc() {
        return findRecent(PageRequest.of(0, 50));
    }

    default List<SourceMaterial> findTop20ByInstrumentOrderByPublishedAtDesc(
        com.alpaka.stock.domain.Instrument instrument
    ) {
        return findRecentByInstrument(instrument, PageRequest.of(0, 20));
    }

    @Query("select material from SourceMaterial material where material.instrument = :instrument order by case when material.publishedAt is null then 1 else 0 end, material.publishedAt desc, material.fetchedAt desc")
    List<SourceMaterial> findRecentByInstrument(
        @Param("instrument") com.alpaka.stock.domain.Instrument instrument,
        Pageable pageable
    );

    default List<SourceMaterial> findTop20ByInstrumentAndKindOrderByPublishedAtDesc(
        com.alpaka.stock.domain.Instrument instrument,
        SourceKind kind
    ) {
        return findRecentByInstrumentAndKind(instrument, kind, PageRequest.of(0, 20));
    }

    @Query("select material from SourceMaterial material where material.instrument = :instrument and material.kind = :kind order by case when material.publishedAt is null then 1 else 0 end, material.publishedAt desc, material.fetchedAt desc")
    List<SourceMaterial> findRecentByInstrumentAndKind(
        @Param("instrument") com.alpaka.stock.domain.Instrument instrument,
        @Param("kind") SourceKind kind,
        Pageable pageable
    );

    long countByKind(SourceKind kind);
}
