package com.nexusexchange.order.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDto {

    @NotBlank(message = "Symbol is required")
    private String symbol;

    @NotBlank(message = "Side is required")
    private String side;

    @Positive(message = "Price must be positive")
    private Double price;

    @Positive(message = "Quantity must be positive")
    private Double quantity;
}
