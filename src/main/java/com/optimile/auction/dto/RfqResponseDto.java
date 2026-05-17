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
public class RfqResponseDto {

    private UUID id;
    private UUID rfqId;
    private String vendorName;
    private String fileName;
    private String uploadedBy;
    private OffsetDateTime uploadedAt;
    private List<RfqResponseRowDto> rows;
}
