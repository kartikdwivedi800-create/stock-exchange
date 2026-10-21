package com.nexusexchange.wallet.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponseDto {

    private Long id;
    private Long userId;
    private String currency;
    private BigDecimal availableBalance;
    private BigDecimal reservedBalance;
}
