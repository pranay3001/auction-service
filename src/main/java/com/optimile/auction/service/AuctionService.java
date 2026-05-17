package com.optimile.auction.service;

import com.optimile.auction.dto.*;
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
public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final AuctionLaneRepository auctionLaneRepository;
    private final AuctionEventRepository auctionEventRepository;
    private final AuctionBidRepository auctionBidRepository;
    private final AwardDecisionRepository awardDecisionRepository;
    private final VendorRepository vendorRepository;
    private final InternalUserRepository internalUserRepository;
    private final NotificationService notificationService;

    public List<AuctionDto> listAuctions(String status, String type, String search, UUID invitedVendorId) {
        if (invitedVendorId != null) {
            AuctionStatus statusEnum = status != null ? AuctionStatus.valueOf(status.toUpperCase()) : null;
            List<Auction> auctions = statusEnum == null
                    ? auctionRepository.findByInvitedVendorId(invitedVendorId)
                    : auctionRepository.findByInvitedVendorIdAndStatus(invitedVendorId, statusEnum);
            return auctions.stream()
                    .map(this::mapToDtoSummary)
                    .collect(Collectors.toList());
        }

        List<Auction> auctions = auctionRepository.findAll();

        return auctions.stream()
                .filter(a -> status == null || a.getStatus().name().equalsIgnoreCase(status))
                .filter(a -> type == null || a.getType().name().equalsIgnoreCase(type))
                .filter(a -> search == null || a.getTitle().toLowerCase().contains(search.toLowerCase()))
                .sorted((a, b) -> {
                    if (a.getUpdatedAt() == null && b.getUpdatedAt() == null) return 0;
                    if (a.getUpdatedAt() == null) return 1;
                    if (b.getUpdatedAt() == null) return -1;
                    return b.getUpdatedAt().compareTo(a.getUpdatedAt());
                })
                .map(this::mapToDtoSummary)
                .collect(Collectors.toList());
    }

    public AuctionDto getAuction(UUID id) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + id));
        return mapToDto(auction);
    }

    public AuctionDto createAuction(CreateAuctionRequest req, UUID userId) {
        Auction auction = new Auction();
        auction.setTitle(req.getTitle());
        auction.setType(req.getType() != null ? AuctionType.valueOf(req.getType()) : null);
        auction.setStatus(AuctionStatus.DRAFT);
        auction.setCreatedById(userId);
        auction.setBookingId(req.getBookingId());
        auction.setRegion(req.getRegion());
        auction.setContractStartDate(req.getContractStartDate());
        auction.setContractEndDate(req.getContractEndDate());
        auction.setMinBidDecrement(req.getMinBidDecrement());
        auction.setExtensionTriggerMinutes(req.getExtensionTriggerMinutes());
        auction.setExtensionDurationMinutes(req.getExtensionDurationMinutes());
        auction.setMaxExtensions(req.getMaxExtensions());
        auction.setBiddingWindowMinutes(req.getBiddingWindowMinutes());
        auction.setAwardDeadline(req.getAwardDeadline());

        // Look up actor name
        String actorName = "system";
        if (userId != null) {
            InternalUser user = internalUserRepository.findById(userId).orElse(null);
            if (user != null) {
                actorName = user.getName();
                auction.setCreatedByRole(user.getRole().name());
            }
        }

        // Set invited vendors
        if (req.getInvitedVendorIds() != null && !req.getInvitedVendorIds().isEmpty()) {
            List<Vendor> vendors = vendorRepository.findAllById(req.getInvitedVendorIds());
            auction.setInvitedVendors(vendors);
        }

        if (Boolean.TRUE.equals(req.getLaunchNow())) {
            auction.setStatus(AuctionStatus.LIVE);
            auction.setStartAt(OffsetDateTime.now());
        }

        // Save auction first so lanes can reference it
        Auction savedAuction = auctionRepository.save(auction);

        // Create lanes
        if (req.getLanes() != null) {
            for (CreateLaneRequest laneReq : req.getLanes()) {
                AuctionLane lane = new AuctionLane();
                lane.setAuction(savedAuction);
                lane.setLane(laneReq.getLane());
                lane.setVehicleType(laneReq.getVehicleType());
                lane.setCapacityMt(laneReq.getCapacityMt());
                lane.setRateUnit(laneReq.getRateUnit() != null ? RateUnit.valueOf(laneReq.getRateUnit()) : RateUnit.PER_TRIP);
                lane.setCeilingRate(laneReq.getCeilingRate());
                lane.setEstimatedTrips(laneReq.getEstimatedTrips());
                lane.setAllocationMode(laneReq.getAllocationMode() != null ? AllocationMode.valueOf(laneReq.getAllocationMode()) : AllocationMode.SINGLE);
                lane.setL1AllocationPct(laneReq.getL1AllocationPct());
                lane.setL2AllocationPct(laneReq.getL2AllocationPct());
                lane.setL3AllocationPct(laneReq.getL3AllocationPct());
                lane.setRegion(laneReq.getRegion());

                if (Boolean.TRUE.equals(req.getLaunchNow()) && req.getBiddingWindowMinutes() != null) {
                    lane.setTimerEndsAt(OffsetDateTime.now().plusMinutes(req.getBiddingWindowMinutes()));
                }

                // Eligible vendors
                if (laneReq.getEligibleVendorIds() != null && !laneReq.getEligibleVendorIds().isEmpty()) {
                    List<Vendor> eligibleVendors = vendorRepository.findAllById(laneReq.getEligibleVendorIds());
                    lane.setEligibleVendors(eligibleVendors);
                }

                savedAuction.getLanes().add(lane);
            }
        }

        savedAuction = auctionRepository.save(savedAuction);

        // Insert CREATED event
        insertEvent(savedAuction, AuctionEventType.CREATED, "Auction created", actorName);

        // If launchNow: insert LAUNCHED event and notify
        if (Boolean.TRUE.equals(req.getLaunchNow())) {
            insertEvent(savedAuction, AuctionEventType.LAUNCHED, "Auction launched", actorName);
            try {
                notificationService.notifyAuctionLive(savedAuction);
            } catch (Exception e) {
                // Notification service may not be ready
            }
        }

        return mapToDto(savedAuction);
    }

    public AuctionDto launchAuction(UUID id, UUID userId) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + id));

        if (auction.getStatus() != AuctionStatus.DRAFT) {
            throw new BusinessException("Auction cannot be launched from status: " + auction.getStatus());
        }

        String actorName = "system";
        if (userId != null) {
            InternalUser user = internalUserRepository.findById(userId).orElse(null);
            if (user != null) actorName = user.getName();
        }

        auction.setStatus(AuctionStatus.LIVE);
        auction.setStartAt(OffsetDateTime.now());

        OffsetDateTime now = OffsetDateTime.now();
        for (AuctionLane lane : auction.getLanes()) {
            if (auction.getBiddingWindowMinutes() != null) {
                lane.setTimerEndsAt(now.plusMinutes(auction.getBiddingWindowMinutes()));
            }
        }

        Auction saved = auctionRepository.save(auction);
        insertEvent(saved, AuctionEventType.LAUNCHED, "Auction launched", actorName);

        try {
            notificationService.notifyAuctionLive(saved);
        } catch (Exception e) {
            // Notification service may not be ready
        }

        return mapToDto(saved);
    }

    public AuctionDto cancelAuction(UUID id, UUID userId, String reason) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + id));

        if (auction.getStatus() != AuctionStatus.DRAFT
                && auction.getStatus() != AuctionStatus.LIVE
                && auction.getStatus() != AuctionStatus.COMPLETED) {
            throw new BusinessException("Auction cannot be cancelled from status: " + auction.getStatus());
        }

        String actorName = "system";
        if (userId != null) {
            InternalUser user = internalUserRepository.findById(userId).orElse(null);
            if (user != null) actorName = user.getName();
        }

        auction.setStatus(AuctionStatus.CANCELLED);
        Auction saved = auctionRepository.save(auction);
        insertEvent(saved, AuctionEventType.CANCELLED, reason, actorName);

        return mapToDto(saved);
    }

    public AuctionDto completeAuction(UUID id) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + id));

        auction.setStatus(AuctionStatus.COMPLETED);
        auction.setCompletedAt(OffsetDateTime.now());
        Auction saved = auctionRepository.save(auction);
        insertEvent(saved, AuctionEventType.COMPLETED, "Auction completed", "system");

        try {
            notificationService.notifyAuctionCompleted(saved);
        } catch (Exception e) {
            // Notification service may not be ready
        }

        return mapToDto(saved);
    }

    // --- Private helpers ---

    private void insertEvent(Auction auction, AuctionEventType type, String message, String actor) {
        AuctionEvent event = new AuctionEvent();
        event.setAuction(auction);
        event.setEventType(type);
        event.setMessage(message);
        event.setActor(actor);
        auctionEventRepository.save(event);
    }

    private AuctionDto mapToDtoSummary(Auction auction) {
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

        // Invited vendor IDs
        if (auction.getInvitedVendors() != null) {
            dto.setInvitedVendorIds(auction.getInvitedVendors().stream()
                    .map(Vendor::getId)
                    .collect(Collectors.toList()));
        }

        // Summary lanes (id, lane, vehicleType, bidCount, timerEndsAt only)
        if (auction.getLanes() != null) {
            dto.setLanes(auction.getLanes().stream()
                    .map(this::mapLaneToSummaryDto)
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    private AuctionDto mapToDto(Auction auction) {
        AuctionDto dto = mapToDtoSummary(auction);

        // Full lanes with ranking and award decisions
        if (auction.getLanes() != null) {
            dto.setLanes(auction.getLanes().stream()
                    .map(this::mapLaneToDto)
                    .collect(Collectors.toList()));
        }

        // Audit trail
        List<AuctionEvent> events = auctionEventRepository
                .findByAuctionIdOrderByEventTimestampAsc(auction.getId());
        dto.setAuditTrail(events.stream()
                .map(this::mapEventToDto)
                .collect(Collectors.toList()));

        return dto;
    }

    private AuctionLaneDto mapLaneToSummaryDto(AuctionLane lane) {
        AuctionLaneDto dto = new AuctionLaneDto();
        dto.setId(lane.getId());
        dto.setLane(lane.getLane());
        dto.setVehicleType(lane.getVehicleType());
        dto.setBidCount(lane.getBidCount());
        dto.setTimerEndsAt(lane.getTimerEndsAt());
        return dto;
    }

    private AuctionLaneDto mapLaneToDto(AuctionLane lane) {
        AuctionLaneDto dto = new AuctionLaneDto();
        dto.setId(lane.getId());
        dto.setLane(lane.getLane());
        dto.setRegion(lane.getRegion());
        dto.setVehicleType(lane.getVehicleType());
        dto.setCapacityMt(lane.getCapacityMt());
        dto.setRateUnit(lane.getRateUnit() != null ? lane.getRateUnit().name() : null);
        dto.setCeilingRate(lane.getCeilingRate());
        dto.setEstimatedTrips(lane.getEstimatedTrips());
        dto.setBasePriceSource(lane.getBasePriceSource());
        dto.setAllocationMode(lane.getAllocationMode() != null ? lane.getAllocationMode().name() : null);
        dto.setL1AllocationPct(lane.getL1AllocationPct());
        dto.setL2AllocationPct(lane.getL2AllocationPct());
        dto.setL3AllocationPct(lane.getL3AllocationPct());
        dto.setTimerEndsAt(lane.getTimerEndsAt());
        dto.setExtensionCount(lane.getExtensionCount());
        dto.setBidCount(lane.getBidCount());
        dto.setRejectionReason(lane.getRejectionReason());
        dto.setCreatedAt(lane.getCreatedAt());

        // Eligible vendor IDs
        if (lane.getEligibleVendors() != null) {
            dto.setEligibleVendorIds(lane.getEligibleVendors().stream()
                    .map(Vendor::getId)
                    .collect(Collectors.toList()));
        }

        // Bid ranking (current bids sorted by amount ASC)
        List<AuctionBid> bids = auctionBidRepository.findByLaneIdAndIsCurrentTrueOrderByAmountAsc(lane.getId());
        dto.setRanking(bids.stream()
                .map(this::mapBidToDto)
                .collect(Collectors.toList()));

        List<AwardDecision> decisions = awardDecisionRepository.findByLaneId(lane.getId());
        dto.setAwardDecision(decisions.stream()
                .map(this::mapDecisionToDto)
                .collect(Collectors.toList()));

        return dto;
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

    private AuctionBidDto mapBidToDto(AuctionBid bid) {
        AuctionBidDto dto = new AuctionBidDto();
        dto.setId(bid.getId());
        dto.setVendorId(bid.getVendorId());
        dto.setVendorName(bid.getVendorName());
        dto.setAmount(bid.getAmount());
        dto.setBidRank(bid.getBidRank());
        dto.setIsCurrent(bid.getIsCurrent());
        dto.setPlacedAt(bid.getPlacedAt());
        return dto;
    }

    private AuctionEventDto mapEventToDto(AuctionEvent e) {
        AuctionEventDto dto = new AuctionEventDto();
        dto.setId(e.getId());
        dto.setEventType(e.getEventType() != null ? e.getEventType().name() : null);
        dto.setMessage(e.getMessage());
        dto.setActor(e.getActor());
        dto.setEventTimestamp(e.getEventTimestamp());
        return dto;
    }
}
