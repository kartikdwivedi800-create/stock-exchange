package com.nexusexchange.order.entity;

import com.nexusexchange.common.enums.OrderSide;
import com.nexusexchange.common.enums.OrderStatus;
import com.nexusexchange.common.enums.OrderType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Mirrors table {@code orders} (migration V2). One row per order; aggregate root of the lifecycle. */
@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Client-supplied idempotency key; unique per user. */
    @Column(name = "client_order_id", length = 64)
    private String clientOrderId;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private OrderSide side;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 10)
    private OrderType orderType;

    /** Null for MARKET orders. */
    @Column(precision = 19, scale = 4)
    private BigDecimal price;

    @Column(nullable = false)
    private Long quantity;

    @Column(name = "filled_quantity", nullable = false)
    private Long filledQuantity;

    @Column(name = "avg_fill_price", precision = 19, scale = 4)
    private BigDecimal avgFillPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    /** Optimistic locking: prevents lost updates from concurrent fills/cancels. */
    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        createdAt = now;
        updatedAt = now;
        if (filledQuantity == null) {
            filledQuantity = 0L;
        }
        if (status == null) {
            status = OrderStatus.NEW;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    @Override
    public String toString() {
        return "Order(id=" + id + ", userId=" + userId + ", symbol=" + symbol + ", side=" + side
                + ", orderType=" + orderType + ", quantity=" + quantity
                + ", filledQuantity=" + filledQuantity + ", status=" + status + ")";
    }
}
