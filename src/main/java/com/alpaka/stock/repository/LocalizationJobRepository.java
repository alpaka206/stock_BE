package com.alpaka.stock.repository;

import com.alpaka.stock.domain.LocalizationJob;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalizationJobRepository extends JpaRepository<LocalizationJob, UUID> {
}
