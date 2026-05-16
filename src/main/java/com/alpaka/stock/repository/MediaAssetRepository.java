package com.alpaka.stock.repository;

import com.alpaka.stock.domain.MediaAsset;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {
}
