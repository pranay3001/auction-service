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
public class NotificationDto {

    private UUID id;
    private String category;
    private String title;
    private String message;
    private Boolean isRead;
    private String deepLink;
    private OffsetDateTime createdAt;
}
