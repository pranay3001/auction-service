package com.optimile.auction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuctionBidDto {

    private UUID id;
    private UUID vendorId;
    private String vendorName;
    private BigDecimal amount;
    private Integer bidRank;
    private Boolean isCurrent;
    private OffsetDateTime placedAt;
}
