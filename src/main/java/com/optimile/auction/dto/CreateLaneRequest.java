package com.optimile.auction.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLaneRequest {

    @NotBlank
    private String lane;
    private String vehicleType;
    private BigDecimal capacityMt;
    private String rateUnit;
    private BigDecimal ceilingRate;
    private Integer estimatedTrips;
    private String allocationMode;
    private BigDecimal l1AllocationPct;
    private BigDecimal l2AllocationPct;
    private BigDecimal l3AllocationPct;
    private String region;
    private List<UUID> eligibleVendorIds;
}
