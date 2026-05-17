package com.optimile.auction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RfqDto {

    private UUID id;
    private String title;
    private OffsetDateTime deadline;
    private String status;
    private String messageToVendor;
    private String templateFileName;
    private UUID createdById;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<String> targetEmails;
    private List<SourcingVendorTrackingDto> vendorTracking;
}
