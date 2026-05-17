package com.optimile.auction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRfiRequest {

    @NotBlank
    private String title;

    private String description;
    private OffsetDateTime deadline;

    @NotEmpty
    private List<String> targetEmails;

    private String messageToVendor;
    private String templateFileName;
}
