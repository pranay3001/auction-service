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
public class PriorityAuctionDto {

    private UUID id;
    private String title;
    private String type;
    private String status;
    private OffsetDateTime updatedAt;
    private OffsetDateTime awardDeadline;
}
