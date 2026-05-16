package com.alpaka.stock.repository;

import com.alpaka.stock.domain.UserAccount;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByProviderIgnoreCaseAndProviderUserId(String provider, String providerUserId);

    Optional<UserAccount> findByEmailIgnoreCase(String email);
}
