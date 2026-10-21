package com.nexusexchange.order.repository;

import com.nexusexchange.common.enums.OrderSide;
import com.nexusexchange.common.enums.OrderStatus;
import com.nexusexchange.common.enums.OrderType;
import com.nexusexchange.testsupport.PostgresRepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.nexusexchange.order.entity.Order;
import com.nexusexchange.order.entity.OrderFill;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;

/** order_db: V2 migration, entity mapping and order rules enforced by the database. */
@PostgresRepositoryTest
class OrderRepositoryTest {

    @Autowired OrderRepository orders;
    @Autowired OrderFillRepository fills;

    private Order limitBuy(String clientOrderId) {
        return Order.builder().userId(1L).clientOrderId(clientOrderId).symbol("TCS").side(OrderSide.BUY)
                .orderType(OrderType.LIMIT).price(new BigDecimal("3500")).quantity(10L).build();
    }

    @Test
    void newOrderDefaults() {
        Order saved = orders.saveAndFlush(limitBuy("c1"));

        assertThat(saved.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(saved.getFilledQuantity()).isZero();
        assertThat(orders.findByUserIdAndClientOrderId(1L, "c1")).isPresent();
        assertThat(orders.findByUserIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 10))).hasSize(1);
    }

    @Test
    void clientOrderIdIsUniquePerUser() {
        orders.saveAndFlush(limitBuy("c1"));

        assertThatThrownBy(() -> orders.saveAndFlush(limitBuy("c1")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_orders_user_client_order");
    }

    @Test
    void marketOrderMustNotHaveAPrice() {
        Order market = Order.builder().userId(1L).symbol("TCS").side(OrderSide.BUY)
                .orderType(OrderType.MARKET).price(new BigDecimal("3500")).quantity(1L).build();

        assertThatThrownBy(() -> orders.saveAndFlush(market))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_orders_market_has_no_price");
    }

    @Test
    void sameTradeCannotBeAppliedTwice() {
        Order order = orders.saveAndFlush(limitBuy("c1"));
        fills.saveAndFlush(OrderFill.builder().order(order).tradeId(9817L).price(new BigDecimal("3480"))
                .quantity(4L).executedAt(Instant.now()).build());

        assertThatThrownBy(() -> fills.saveAndFlush(OrderFill.builder().order(order).tradeId(9817L)
                .price(new BigDecimal("3480")).quantity(4L).executedAt(Instant.now()).build()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_order_fills_order_trade");
    }
}
