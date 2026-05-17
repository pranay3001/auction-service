package com.optimile.auction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingDto {

    private UUID id;
    private String lane;
    private String vehicleType;
    private String commodity;
    private BigDecimal quantity;
    private String uom;
    private LocalDate loadingDate;
    private String status;
    private java.time.OffsetDateTime createdAt;
}
