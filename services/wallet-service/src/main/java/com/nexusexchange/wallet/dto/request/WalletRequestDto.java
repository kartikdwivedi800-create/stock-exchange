package com.nexusexchange.wallet.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletRequestDto {

    @NotNull(message = "userId is required")
    private Long userId;

    /** ISO-4217 currency code; defaults to INR when not supplied. */
    private String currency;

    @Positive(message = "Initial deposit amount must be positive")
    private BigDecimal availableBalance;
}
