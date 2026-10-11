package com.nexusexchange.stock.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockRequestDto {

    @NotBlank(message = "Symbol is required")
    @Size(max = 20, message = "Symbol must be at most 20 characters")
    private String symbol;

    @NotBlank(message = "Company name is required")
    @Size(max = 255, message = "Company name must be at most 255 characters")
    private String companyName;

    @Size(max = 50, message = "Sector must be at most 50 characters")
    private String sector;

    /** Optional; defaults to 0.05 when omitted. */
    @Positive(message = "Tick size must be positive")
    private BigDecimal tickSize;

    /** Optional; defaults to 1 when omitted. */
    @Positive(message = "Lot size must be positive")
    private Integer lotSize;
}
