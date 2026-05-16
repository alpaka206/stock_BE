package com.alpaka.stock.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "user_accounts")
public class UserAccount extends BaseEntity {
    @Column(nullable = false, length = 40)
    private String provider;

    @Column(nullable = false, length = 160)
    private String providerUserId;

    @Column(nullable = false, length = 240)
    private String email;

    @Column(nullable = false, length = 160)
    private String displayName;

    @Column(nullable = false, length = 16)
    private String locale;

    @Column(nullable = false, length = 40)
    private String role;

    @Column(nullable = false)
    private boolean active = true;

    private OffsetDateTime lastLoginAt;

    protected UserAccount() {
    }

    public UserAccount(
        String provider,
        String providerUserId,
        String email,
        String displayName,
        String locale,
        String role
    ) {
        this.provider = provider;
        this.providerUserId = providerUserId;
        this.email = email;
        this.displayName = displayName;
        this.locale = locale;
        this.role = role;
    }

    public void updateProfile(String email, String displayName, String locale) {
        this.email = email;
        this.displayName = displayName;
        this.locale = locale;
        this.lastLoginAt = OffsetDateTime.now();
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLocale() {
        return locale;
    }

    public String getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

    public OffsetDateTime getLastLoginAt() {
        return lastLoginAt;
    }
}
