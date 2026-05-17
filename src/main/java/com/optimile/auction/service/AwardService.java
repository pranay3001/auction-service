package com.optimile.auction.service;

import com.optimile.auction.dto.AuctionDto;
import com.optimile.auction.dto.AwardDecisionDto;
import com.optimile.auction.dto.AwardDecisionRequest;
import com.optimile.auction.dto.ContractDto;
import com.optimile.auction.exception.BusinessException;
import com.optimile.auction.exception.ResourceNotFoundException;
import com.optimile.auction.model.entity.*;
import com.optimile.auction.model.enums.*;
import com.optimile.auction.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class AwardService {

    private final AuctionRepository auctionRepository;
    private final AuctionLaneRepository auctionLaneRepository;
    private final AwardDecisionRepository awardDecisionRepository;
    private final ContractRepository contractRepository;
    private final AuctionEventRepository auctionEventRepository;
    private final BookingRepository bookingRepository;

    public List<AwardDecisionDto> awardAuction(UUID auctionId, List<AwardDecisionRequest> requests) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));

        if (auction.getStatus() != AuctionStatus.LIVE
                && auction.getStatus() != AuctionStatus.COMPLETED) {
            throw new BusinessException("Auction must be LIVE or COMPLETED to award; current status: "
                    + auction.getStatus());
        }

        // Delete existing award decisions for this auction
        awardDecisionRepository.deleteByAuctionId(auctionId);

        // Insert new award decisions
        List<AwardDecision> savedDecisions = new ArrayList<>();
        for (AwardDecisionRequest req : requests) {
            AuctionLane lane = auctionLaneRepository.findById(req.getLaneId())
                    .orElseThrow(() -> new ResourceNotFoundException("Lane not found: " + req.getLaneId()));

            AwardDecision decision = new AwardDecision();
            decision.setAuction(auction);
            decision.setLane(lane);
            decision.setVendorId(req.getVendorId());
            decision.setVendorName(req.getVendorName());
            decision.setAllocationRank(req.getAllocationRank() != null ? AllocationRank.valueOf(req.getAllocationRank()) : null);
            decision.setAwardedBidRank(req.getAwardedBidRank() != null ? AllocationRank.valueOf(req.getAwardedBidRank()) : null);
            decision.setAwardedAmount(req.getAwardedAmount());
            decision.setOverrideReason(req.getOverrideReason());
            decision.setAllocationPercent(req.getAllocationPercent());
            savedDecisions.add(awardDecisionRepository.save(decision));
        }

        // Set auction status to AWARDED
        auction.setStatus(AuctionStatus.AWARDED);
        auctionRepository.save(auction);

        // Insert AWARDED event
        insertEvent(auction, AuctionEventType.AWARDED, "Auction awarded", "system");

        // Insert OVERRIDE events for any request where overrideReason != null
        for (AwardDecisionRequest req : requests) {
            if (req.getOverrideReason() != null && !req.getOverrideReason().isBlank()) {
                insertEvent(auction, AuctionEventType.OVERRIDE,
                        "Override for vendor " + req.getVendorName() + ": " + req.getOverrideReason(),
                        "system");
            }
        }

        return savedDecisions.stream()
                .map(this::mapDecisionToDto)
                .collect(Collectors.toList());
    }

    public List<ContractDto> finalizeAuction(UUID auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));

        if (auction.getStatus() != AuctionStatus.AWARDED) {
            throw new BusinessException("Auction must be AWARDED to finalize; current status: "
                    + auction.getStatus());
        }

        // Load award decisions for this auction
        List<AwardDecision> decisions = awardDecisionRepository.findByAuctionId(auctionId);

        // Map auction type to contract type
        ContractType contractType = mapAuctionTypeToContractType(auction.getType());

        List<Contract> contracts = new ArrayList<>();
        for (AwardDecision decision : decisions) {
            AuctionLane lane = decision.getLane();

            Contract contract = new Contract();
            contract.setSourceAuctionId(auctionId);
            contract.setContractType(contractType);
            contract.setVendorId(decision.getVendorId());
            contract.setVendorName(decision.getVendorName());
            contract.setLane(lane.getLane());
            contract.setRegion(lane.getRegion());
            contract.setVehicleType(lane.getVehicleType());
            contract.setRateUnit(lane.getRateUnit());
            contract.setEstimatedTrips(lane.getEstimatedTrips());
            contract.setContractedRate(decision.getAwardedAmount());
            contract.setVolumeAllocationPercent(decision.getAllocationPercent());
            contract.setAllocationRank(decision.getAllocationRank());
            contract.setStartDate(auction.getContractStartDate());
            contract.setEndDate(auction.getContractEndDate());
            contract.setStatus(ContractStatus.ACTIVE);
            contract.setL1OverrideReason(decision.getOverrideReason());

            Contract saved = contractRepository.save(contract);
            contracts.add(saved);

            // Insert CONTRACT_CREATED event
            insertEvent(auction, AuctionEventType.CONTRACT_CREATED,
                    "Contract created for vendor " + decision.getVendorName()
                            + " on lane " + lane.getLane(),
                    "system");
        }

        // If auction.bookingId != null: update booking status = READY_FOR_DISPATCH
        if (auction.getBookingId() != null) {
            bookingRepository.findById(auction.getBookingId()).ifPresent(booking -> {
                booking.setStatus(BookingStatus.READY_FOR_DISPATCH);
                bookingRepository.save(booking);
            });
        }

        return contracts.stream()
                .map(this::mapContractToDto)
                .collect(Collectors.toList());
    }

    public AuctionDto rejectAuction(UUID auctionId, String reason) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));

        auction.setStatus(AuctionStatus.NO_BIDS);
        Auction saved = auctionRepository.save(auction);
        insertEvent(saved, AuctionEventType.CANCELLED, reason != null ? reason : "Rejected - no bids", "system");

        return mapAuctionToDto(saved);
    }

    // --- Private helpers ---

    private ContractType mapAuctionTypeToContractType(AuctionType type) {
        if (type == null) return ContractType.BULK;
        switch (type) {
            case LOT: return ContractType.LOT;
            case SPOT:
            case BULK:
            default: return ContractType.BULK;
        }
    }

    private void insertEvent(Auction auction, AuctionEventType type, String message, String actor) {
        AuctionEvent event = new AuctionEvent();
        event.setAuction(auction);
        event.setEventType(type);
        event.setMessage(message);
        event.setActor(actor);
        auctionEventRepository.save(event);
    }

    private AwardDecisionDto mapDecisionToDto(AwardDecision d) {
        AwardDecisionDto dto = new AwardDecisionDto();
        dto.setId(d.getId());
        dto.setAuctionId(d.getAuction().getId());
        dto.setLaneId(d.getLane().getId());
        dto.setVendorId(d.getVendorId());
        dto.setVendorName(d.getVendorName());
        dto.setAllocationRank(d.getAllocationRank() != null ? d.getAllocationRank().name() : null);
        dto.setAwardedBidRank(d.getAwardedBidRank() != null ? d.getAwardedBidRank().name() : null);
        dto.setAwardedAmount(d.getAwardedAmount());
        dto.setOverrideReason(d.getOverrideReason());
        dto.setAllocationPercent(d.getAllocationPercent());
        dto.setDecidedAt(d.getDecidedAt());
        return dto;
    }

    private ContractDto mapContractToDto(Contract c) {
        ContractDto dto = new ContractDto();
        dto.setId(c.getId());
        dto.setSourceAuctionId(c.getSourceAuctionId());
        dto.setContractType(c.getContractType() != null ? c.getContractType().name() : null);
        dto.setVendorId(c.getVendorId());
        dto.setVendorName(c.getVendorName());
        dto.setLane(c.getLane());
        dto.setRegion(c.getRegion());
        dto.setVehicleType(c.getVehicleType());
        dto.setContractedRate(c.getContractedRate());
        dto.setRateUnit(c.getRateUnit() != null ? c.getRateUnit().name() : null);
        dto.setVolumeAllocationPercent(c.getVolumeAllocationPercent());
        dto.setAllocationRank(c.getAllocationRank() != null ? c.getAllocationRank().name() : null);
        dto.setStartDate(c.getStartDate());
        dto.setEndDate(c.getEndDate());
        dto.setEstimatedTrips(c.getEstimatedTrips());
        dto.setStatus(c.getStatus() != null ? c.getStatus().name() : null);
        dto.setL1OverrideReason(c.getL1OverrideReason());
        dto.setRateSyncedToTms(c.getRateSyncedToTms());
        dto.setRateDeviationOpen(c.getRateDeviationOpen());
        dto.setCreatedAt(c.getCreatedAt());
        return dto;
    }

    private AuctionDto mapAuctionToDto(Auction auction) {
        AuctionDto dto = new AuctionDto();
        dto.setId(auction.getId());
        dto.setTitle(auction.getTitle());
        dto.setType(auction.getType() != null ? auction.getType().name() : null);
        dto.setStatus(auction.getStatus() != null ? auction.getStatus().name() : null);
        dto.setCreatedById(auction.getCreatedById());
        dto.setCreatedByRole(auction.getCreatedByRole());
        dto.setCreatedAt(auction.getCreatedAt());
        dto.setUpdatedAt(auction.getUpdatedAt());
        dto.setStartAt(auction.getStartAt());
        dto.setCompletedAt(auction.getCompletedAt());
        dto.setContractStartDate(auction.getContractStartDate());
        dto.setContractEndDate(auction.getContractEndDate());
        dto.setMinBidDecrement(auction.getMinBidDecrement());
        dto.setExtensionTriggerMinutes(auction.getExtensionTriggerMinutes());
        dto.setExtensionDurationMinutes(auction.getExtensionDurationMinutes());
        dto.setMaxExtensions(auction.getMaxExtensions());
        dto.setBiddingWindowMinutes(auction.getBiddingWindowMinutes());
        dto.setBookingId(auction.getBookingId());
        dto.setRegion(auction.getRegion());
        dto.setAwardDeadline(auction.getAwardDeadline());
        return dto;
    }
}
