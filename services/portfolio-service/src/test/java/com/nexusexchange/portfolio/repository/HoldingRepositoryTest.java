package com.nexusexchange.portfolio.repository;

import com.nexusexchange.testsupport.PostgresRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.nexusexchange.portfolio.entity.Holding;
import com.nexusexchange.portfolio.entity.ReservationStatus;
import com.nexusexchange.portfolio.entity.ShareReservation;

/** portfolio_db: V2 migration, entity mapping and share-reservation rules. */
@PostgresRepositoryTest
class HoldingRepositoryTest {

    @Autowired HoldingRepository holdings;
    @Autowired ShareReservationRepository reservations;

    @Test
    void newHoldingDefaults() {
        Holding saved = holdings.saveAndFlush(Holding.builder().userId(1L).symbol("TCS").quantity(10L).build());

        assertThat(saved.getReservedQuantity()).isZero();
        assertThat(saved.getAverageBuyPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(holdings.findByUserIdAndSymbol(1L, "TCS")).isPresent();
        assertThat(holdings.findByUserId(1L)).hasSize(1);
    }

    @Test
    void oneHoldingPerUserAndSymbol() {
        holdings.saveAndFlush(Holding.builder().userId(1L).symbol("TCS").quantity(10L).build());

        assertThatThrownBy(() -> holdings.saveAndFlush(Holding.builder().userId(1L).symbol("TCS").quantity(5L).build()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_holdings_user_symbol");
    }

    @Test
    void cannotReserveMoreSharesThanHeld() {
        Holding holding = holdings.saveAndFlush(Holding.builder().userId(1L).symbol("TCS").quantity(10L).build());
        holding.setReservedQuantity(11L);

        assertThatThrownBy(() -> holdings.saveAndFlush(holding))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_holdings_reserved_within_quantity");
    }

    @Test
    void reservationDefaultsAndOnePerOrder() {
        Holding holding = holdings.saveAndFlush(Holding.builder().userId(1L).symbol("TCS").quantity(10L).build());
        ShareReservation reservation = reservations.saveAndFlush(ShareReservation.builder()
                .orderId(42L).holding(holding).quantityReserved(4L).build());

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(reservation.getQuantityUsed()).isZero();
        assertThatThrownBy(() -> reservations.saveAndFlush(ShareReservation.builder()
                .orderId(42L).holding(holding).quantityReserved(2L).build()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_share_reservations_order");
    }
}
