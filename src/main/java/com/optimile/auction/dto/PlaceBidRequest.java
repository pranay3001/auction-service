package com.optimile.auction.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceBidRequest {

    @NotNull
    private UUID vendorId;

    private String vendorName;

    @NotNull
    private BigDecimal amount;
}
