package com.nexusexchange.wallet.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletRequestDto {

    @NotBlank(message = "Currency is required")
    private String currency;

    @PositiveOrZero(message = "Balance must be zero or positive")
    private Double balance;
}
