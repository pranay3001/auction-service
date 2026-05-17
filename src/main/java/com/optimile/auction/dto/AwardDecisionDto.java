package com.optimile.auction.dto;

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
public class AwardDecisionDto {

    private UUID id;
    private UUID auctionId;
    private UUID laneId;
    private UUID vendorId;
    private String vendorName;
    private String allocationRank;
    private String awardedBidRank;
    private BigDecimal awardedAmount;
    private String overrideReason;
    private BigDecimal allocationPercent;
    private java.time.OffsetDateTime decidedAt;
}
