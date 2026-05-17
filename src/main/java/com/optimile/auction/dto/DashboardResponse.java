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
public class DashboardResponse {

    private KpiDto liveAuctions;
    private KpiDto pendingAwards;
    private KpiDto expiringContracts;
    private List<PriorityAuctionDto> priorityAuctions;
    private List<ExpiringContractDto> expiringContractsList;
}
