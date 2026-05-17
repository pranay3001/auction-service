package com.optimile.auction.dto;

import jakarta.validation.constraints.NotBlank;
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
public class AwardDecisionRequest {

    @NotNull
    private UUID laneId;

    @NotNull
    private UUID vendorId;

    private String vendorName;

    @NotBlank
    private String allocationRank;

    private String awardedBidRank;

    @NotNull
    private BigDecimal awardedAmount;

    private String overrideReason;
    private BigDecimal allocationPercent;
}
