package com.alpaka.stock.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "refresh_sessions")
public class RefreshSession extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(nullable = false, unique = true, length = 96)
    private String tokenHash;

    @Column(length = 512)
    private String userAgent;

    @Column(length = 80)
    private String ipAddress;

    @Column(nullable = false)
    private OffsetDateTime expiresAt;

    private OffsetDateTime revokedAt;

    protected RefreshSession() {
    }

    public RefreshSession(
        UserAccount user,
        String tokenHash,
        String userAgent,
        String ipAddress,
        OffsetDateTime expiresAt
    ) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.userAgent = userAgent;
        this.ipAddress = ipAddress;
        this.expiresAt = expiresAt;
    }

    public void revoke() {
        this.revokedAt = OffsetDateTime.now();
    }

    public UserAccount getUser() {
        return user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getRevokedAt() {
        return revokedAt;
    }

    public boolean isUsable(OffsetDateTime now) {
        return revokedAt == null && expiresAt.isAfter(now) && user.isActive();
    }
}
