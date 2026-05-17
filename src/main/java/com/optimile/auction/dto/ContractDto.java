package com.optimile.auction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractDto {

    private UUID id;
    private UUID sourceAuctionId;
    private String contractType;
    private UUID vendorId;
    private String vendorName;
    private String lane;
    private String region;
    private String vehicleType;
    private BigDecimal contractedRate;
    private String rateUnit;
    private BigDecimal volumeAllocationPercent;
    private String allocationRank;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer estimatedTrips;
    private String status;
    private String l1OverrideReason;
    private Boolean rateSyncedToTms;
    private Boolean rateDeviationOpen;
    private OffsetDateTime createdAt;
    private List<PlacementFailureDto> placementFailures;
}
