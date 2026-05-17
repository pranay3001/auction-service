package com.optimile.auction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAuctionRequest {

    @NotBlank
    private String title;

    @NotNull
    private String type;

    private UUID bookingId;
    private String region;
    private BigDecimal minBidDecrement;
    private Integer extensionTriggerMinutes;
    private Integer extensionDurationMinutes;
    private Integer maxExtensions;
    private Integer biddingWindowMinutes;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private java.time.OffsetDateTime awardDeadline;
    private List<UUID> invitedVendorIds;
    private Boolean launchNow;

    @NotEmpty
    private List<CreateLaneRequest> lanes;
}
