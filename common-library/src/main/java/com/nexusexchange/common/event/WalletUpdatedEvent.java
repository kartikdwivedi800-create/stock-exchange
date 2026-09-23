package com.nexusexchange.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WalletUpdatedEvent extends BaseEvent {
    private Long walletId;
    private Long userId;
    private String currency;
    private BigDecimal balance;
    private BigDecimal amountChanged;
    private String transactionType;
}
