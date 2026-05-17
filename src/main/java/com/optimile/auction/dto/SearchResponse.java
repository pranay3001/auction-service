package com.optimile.auction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchResponse {

    private List<PriorityAuctionDto> auctions;
    private List<ExpiringContractDto> contracts;
}
