package com.optimile.auction.service;

import com.optimile.auction.dto.*;
import com.optimile.auction.model.entity.Auction;
import com.optimile.auction.model.entity.Contract;
import com.optimile.auction.model.enums.AuctionStatus;
import com.optimile.auction.model.enums.ContractStatus;
import com.optimile.auction.repository.AuctionRepository;
import com.optimile.auction.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardService {

    private final AuctionRepository auctionRepository;
    private final ContractRepository contractRepository;

    public DashboardResponse getDashboard() {
        int liveCount = auctionRepository.findByStatus(AuctionStatus.LIVE).size();
        int pendingCount = auctionRepository.findByStatus(AuctionStatus.COMPLETED).size();
        List<Contract> expiring = contractRepository.findByStatus(ContractStatus.EXPIRING_SOON);

        List<PriorityAuctionDto> priority = auctionRepository
                .findTop4ByStatusInOrderByUpdatedAtDesc(List.of(AuctionStatus.LIVE, AuctionStatus.COMPLETED))
                .stream()
                .map(this::mapToPriorityDto)
                .toList();

        List<ExpiringContractDto> expiringList = expiring.stream()
                .map(this::mapToExpiringDto)
                .toList();

        return DashboardResponse.builder()
                .liveAuctions(KpiDto.builder()
                        .value(liveCount)
                        .insight(liveCount + " auctions currently accepting bids")
                        .build())
                .pendingAwards(KpiDto.builder()
                        .value(pendingCount)
                        .insight(pendingCount + " auctions awaiting award confirmation")
                        .build())
                .expiringContracts(KpiDto.builder()
                        .value(expiring.size())
                        .insight(expiring.size() + " contracts expire within 30 days")
                        .build())
                .priorityAuctions(priority)
                .expiringContractsList(expiringList)
                .build();
    }

    // --- Private helpers ---

    private PriorityAuctionDto mapToPriorityDto(Auction auction) {
        return PriorityAuctionDto.builder()
                .id(auction.getId())
                .title(auction.getTitle())
                .type(auction.getType() != null ? auction.getType().name() : null)
                .status(auction.getStatus() != null ? auction.getStatus().name() : null)
                .updatedAt(auction.getUpdatedAt())
                .awardDeadline(auction.getAwardDeadline())
                .build();
    }

    private ExpiringContractDto mapToExpiringDto(Contract contract) {
        return ExpiringContractDto.builder()
                .id(contract.getId())
                .vendorName(contract.getVendorName())
                .lane(contract.getLane())
                .endDate(contract.getEndDate())
                .status(contract.getStatus() != null ? contract.getStatus().name() : null)
                .build();
    }
}
