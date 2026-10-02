package com.nexusexchange.wallet.entity;

import com.nexusexchange.common.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Mirrors table {@code wallet_transactions} (migration V2). Append-only ledger row per balance movement. */
@Entity
@Table(name = "wallet_transactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    /** Movement type; values must align with the CHECK constraint in V2. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    /** Always positive — the magnitude of the movement. */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /** Available balance on the wallet immediately after this transaction. */
    @Column(name = "available_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal availableAfter;

    /** Reserved balance on the wallet immediately after this transaction. */
    @Column(name = "reserved_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal reservedAfter;

    /** Broad category of the entity that triggered this movement (e.g. ORDER, TRADE). */
    @Column(name = "reference_type", length = 20)
    private String referenceType;

    /** Identifier of the triggering entity (order id, trade id, …). */
    @Column(name = "reference_id", length = 64)
    private String referenceId;

    /** Client-supplied key that prevents double-processing the same operation. */
    @Column(name = "idempotency_key", unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        }
    }
}
