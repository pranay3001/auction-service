package com.optimile.auction.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadRfqResponseRequest {

    private String fileName;
    private String vendorName;

    @NotEmpty
    private List<RfqResponseRowDto> rows;
}
