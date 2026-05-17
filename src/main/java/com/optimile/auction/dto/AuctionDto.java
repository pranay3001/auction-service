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
public class AuctionDto {

    private UUID id;
    private String title;
    private String type;
    private String status;
    private UUID createdById;
    private String createdByRole;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime startAt;
    private OffsetDateTime completedAt;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private BigDecimal minBidDecrement;
    private Integer extensionTriggerMinutes;
    private Integer extensionDurationMinutes;
    private Integer maxExtensions;
    private Integer biddingWindowMinutes;
    private UUID bookingId;
    private String region;
    private OffsetDateTime awardDeadline;
    private List<UUID> invitedVendorIds;
    private List<AuctionLaneDto> lanes;
    private List<AuctionEventDto> auditTrail;
}
