package com.nexusexchange.wallet.repository;

import com.nexusexchange.common.enums.TransactionType;
import com.nexusexchange.testsupport.PostgresRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.nexusexchange.wallet.entity.FundReservation;
import com.nexusexchange.wallet.entity.OutboxEvent;
import com.nexusexchange.wallet.entity.ReservationStatus;
import com.nexusexchange.wallet.entity.Wallet;
import com.nexusexchange.wallet.entity.WalletTransaction;

/** wallet_db: V2 migration, entity mapping and money constraints. */
@PostgresRepositoryTest
class WalletRepositoryTest {

    @Autowired WalletRepository wallets;
    @Autowired WalletTransactionRepository transactions;
    @Autowired FundReservationRepository reservations;
    @Autowired OutboxEventRepository outbox;

    @Test
    void newWalletStartsEmptyInInr() {
        Wallet wallet = wallets.saveAndFlush(Wallet.builder().userId(1L).build());

        assertThat(wallet.getCurrency()).isEqualTo("INR");
        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("0");
        assertThat(wallet.getReservedBalance()).isEqualByComparingTo("0");
        assertThat(wallets.findByUserId(1L)).isPresent();
    }

    @Test
    void oneWalletPerUser() {
        wallets.saveAndFlush(Wallet.builder().userId(1L).build());

        assertThatThrownBy(() -> wallets.saveAndFlush(Wallet.builder().userId(1L).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void balanceCannotGoNegative() {
        Wallet wallet = wallets.saveAndFlush(Wallet.builder().userId(1L).build());
        wallet.setAvailableBalance(new BigDecimal("-0.01"));

        assertThatThrownBy(() -> wallets.saveAndFlush(wallet))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void reservationAndLedgerRow() {
        Wallet wallet = wallets.saveAndFlush(Wallet.builder().userId(1L)
                .availableBalance(new BigDecimal("6500")).reservedBalance(new BigDecimal("3500")).build());
        FundReservation reservation = reservations.saveAndFlush(FundReservation.builder()
                .orderId(42L).wallet(wallet).amountReserved(new BigDecimal("3500")).build());
        transactions.saveAndFlush(WalletTransaction.builder().wallet(wallet).type(TransactionType.HOLD)
                .amount(new BigDecimal("3500")).availableAfter(new BigDecimal("6500"))
                .reservedAfter(new BigDecimal("3500")).referenceType("ORDER").referenceId("42").build());

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(reservation.getAmountUsed()).isEqualByComparingTo("0");
        assertThat(reservations.findByOrderId(42L)).isPresent();
        assertThat(transactions.count()).isEqualTo(1);
    }

    @Test
    void cannotUseMoreThanReserved() {
        Wallet wallet = wallets.saveAndFlush(Wallet.builder().userId(1L).build());
        FundReservation reservation = FundReservation.builder().orderId(42L).wallet(wallet)
                .amountReserved(new BigDecimal("100")).amountUsed(new BigDecimal("150")).build();

        assertThatThrownBy(() -> reservations.saveAndFlush(reservation))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void outboxStoresJsonPayload() {
        OutboxEvent saved = outbox.saveAndFlush(OutboxEvent.builder().aggregateType("Wallet").aggregateId("1")
                .eventType("WalletUpdated").topic("wallet-events").messageKey("1")
                .payload("{\"userId\":1,\"balance\":\"6500.0000\"}").build());

        assertThat(saved.getId()).isNotNull();
        assertThat(outbox.findById(saved.getId()).orElseThrow().getPayload()).contains("\"userId\"");
    }
}
