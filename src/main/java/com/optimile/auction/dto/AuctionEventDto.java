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
public class AuctionEventDto {

    private UUID id;
    private String eventType;
    private String message;
    private String actor;
    private OffsetDateTime eventTimestamp;
}
