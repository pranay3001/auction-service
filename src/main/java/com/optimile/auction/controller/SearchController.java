package com.optimile.auction.controller;

import com.optimile.auction.dto.ExpiringContractDto;
import com.optimile.auction.dto.PriorityAuctionDto;
import com.optimile.auction.dto.SearchResponse;
import com.optimile.auction.model.entity.Auction;
import com.optimile.auction.model.entity.Contract;
import com.optimile.auction.repository.AuctionRepository;
import com.optimile.auction.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final AuctionRepository auctionRepository;
    private final ContractRepository contractRepository;

    @GetMapping
    public ResponseEntity<SearchResponse> search(@RequestParam String q) {
        String lowerQ = q.toLowerCase();

        List<PriorityAuctionDto> auctions = auctionRepository.findAll().stream()
                .filter(a -> a.getTitle() != null && a.getTitle().toLowerCase().contains(lowerQ))
                .map(this::mapToPriorityDto)
                .toList();

        List<ExpiringContractDto> contracts = contractRepository.findAll().stream()
                .filter(c -> (c.getVendorName() != null && c.getVendorName().toLowerCase().contains(lowerQ))
                        || (c.getLane() != null && c.getLane().toLowerCase().contains(lowerQ)))
                .map(this::mapToExpiringDto)
                .toList();

        return ResponseEntity.ok(SearchResponse.builder()
                .auctions(auctions)
                .contracts(contracts)
                .build());
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
