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
public class PlacementFailureDto {

    private UUID id;
    private String failedVendor;
    private String replacementVendor;
    private BigDecimal originalRate;
    private BigDecimal replacementRate;
    private BigDecimal differential;
    private Boolean debitNoteTriggered;
    private OffsetDateTime recordedAt;
}
