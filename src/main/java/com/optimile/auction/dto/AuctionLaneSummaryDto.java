package com.optimile.auction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuctionLaneSummaryDto {

    private UUID id;
    private String lane;
    private String vehicleType;
    private Integer bidCount;
    private OffsetDateTime timerEndsAt;
}
