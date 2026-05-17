package com.optimile.auction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuctionLaneDto {

    private UUID id;
    private String lane;
    private String region;
    private String vehicleType;
    private BigDecimal capacityMt;
    private String rateUnit;
    private BigDecimal ceilingRate;
    private Integer estimatedTrips;
    private String basePriceSource;
    private String allocationMode;
    private BigDecimal l1AllocationPct;
    private BigDecimal l2AllocationPct;
    private BigDecimal l3AllocationPct;
    private OffsetDateTime timerEndsAt;
    private Integer extensionCount;
    private Integer bidCount;
    private String rejectionReason;
    private List<AuctionBidDto> ranking;
    private List<AwardDecisionDto> awardDecision;
    private List<UUID> eligibleVendorIds;
    private OffsetDateTime createdAt;
}
