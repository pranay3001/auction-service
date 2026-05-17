package com.optimile.auction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorDto {

    private UUID id;
    private String name;
    private String email;
    private BigDecimal score;
    private java.time.OffsetDateTime createdAt;
}
