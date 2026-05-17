package com.optimile.auction.service;

import com.optimile.auction.dto.AuctionBidDto;
import com.optimile.auction.dto.PlaceBidRequest;
import com.optimile.auction.exception.BusinessException;
import com.optimile.auction.exception.ResourceNotFoundException;
import com.optimile.auction.model.entity.Auction;
import com.optimile.auction.model.entity.AuctionBid;
import com.optimile.auction.model.entity.AuctionEvent;
import com.optimile.auction.model.entity.AuctionLane;
import com.optimile.auction.model.enums.AuctionEventType;
import com.optimile.auction.model.enums.AuctionStatus;
import com.optimile.auction.repository.AuctionBidRepository;
import com.optimile.auction.repository.AuctionEventRepository;
import com.optimile.auction.repository.AuctionLaneRepository;
import com.optimile.auction.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class BidService {

    private final AuctionRepository auctionRepository;
    private final AuctionLaneRepository auctionLaneRepository;
    private final AuctionBidRepository auctionBidRepository;
    private final AuctionEventRepository auctionEventRepository;

    public List<AuctionBidDto> getBids(UUID auctionId, UUID laneId) {
        List<AuctionBid> bids = auctionBidRepository
                .findByLaneIdAndIsCurrentTrueOrderByAmountAsc(laneId);
        return bids.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public AuctionBidDto placeBid(UUID auctionId, UUID laneId, PlaceBidRequest req) {
        // 1. Load auction — validate status == LIVE
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found: " + auctionId));

        if (auction.getStatus() != AuctionStatus.LIVE) {
            throw new BusinessException("Auction is not LIVE; current status: " + auction.getStatus());
        }

        // 2. Load lane — validate exists in auction
        AuctionLane lane = auctionLaneRepository.findById(laneId)
                .orElseThrow(() -> new ResourceNotFoundException("Lane not found: " + laneId));

        if (!lane.getAuction().getId().equals(auctionId)) {
            throw new BusinessException("Lane does not belong to auction: " + auctionId);
        }

        // 3. Validate amount <= lane.ceilingRate
        if (lane.getCeilingRate() != null && req.getAmount().compareTo(lane.getCeilingRate()) > 0) {
            throw new BusinessException("Bid amount exceeds ceiling rate of " + lane.getCeilingRate());
        }

        // 4. Get all current bids for this lane
        List<AuctionBid> currentBids = auctionBidRepository.findByLaneIdAndIsCurrentTrue(laneId);

        // Check if vendor already has a current bid
        AuctionBid existingVendorBid = currentBids.stream()
                .filter(b -> b.getVendorId().equals(req.getVendorId()))
                .findFirst()
                .orElse(null);

        if (existingVendorBid != null) {
            // Validate new amount <= existingBid.amount - minBidDecrement
            BigDecimal minDecrement = auction.getMinBidDecrement() != null
                    ? auction.getMinBidDecrement() : BigDecimal.ZERO;
            BigDecimal maxAllowed = existingVendorBid.getAmount().subtract(minDecrement);
            if (req.getAmount().compareTo(maxAllowed) > 0) {
                throw new BusinessException("New bid must be at least " + minDecrement
                        + " lower than current bid of " + existingVendorBid.getAmount());
            }
        }

        // 5. Mark vendor's existing current bid as is_current=false (if any)
        if (existingVendorBid != null) {
            existingVendorBid.setIsCurrent(false);
            auctionBidRepository.save(existingVendorBid);
        }

        // 6. Save new bid (is_current=true)
        AuctionBid newBid = new AuctionBid();
        newBid.setAuction(auction);
        newBid.setLane(lane);
        newBid.setVendorId(req.getVendorId());
        newBid.setVendorName(req.getVendorName());
        newBid.setAmount(req.getAmount());
        newBid.setIsCurrent(true);
        newBid = auctionBidRepository.save(newBid);

        // 7. Recompute bid_rank for all is_current bids on lane, sorted by amount ASC
        List<AuctionBid> allCurrentBids = auctionBidRepository.findByLaneIdAndIsCurrentTrue(laneId);
        allCurrentBids.sort((a, b) -> a.getAmount().compareTo(b.getAmount()));
        for (int i = 0; i < allCurrentBids.size(); i++) {
            allCurrentBids.get(i).setBidRank(i + 1);
        }
        auctionBidRepository.saveAll(allCurrentBids);

        // Set rank on the new bid object for return
        for (AuctionBid b : allCurrentBids) {
            if (b.getId().equals(newBid.getId())) {
                newBid.setBidRank(b.getBidRank());
                break;
            }
        }

        // 8. Increment lane.bidCount
        lane.setBidCount(lane.getBidCount() + 1);

        // 9. Timer extension check
        OffsetDateTime now = OffsetDateTime.now();
        if (lane.getTimerEndsAt() != null
                && auction.getExtensionTriggerMinutes() != null
                && auction.getMaxExtensions() != null
                && now.isAfter(lane.getTimerEndsAt().minusMinutes(auction.getExtensionTriggerMinutes()))
                && lane.getExtensionCount() < auction.getMaxExtensions()) {

            int extensionDuration = auction.getExtensionDurationMinutes() != null
                    ? auction.getExtensionDurationMinutes() : 10;
            lane.setTimerEndsAt(lane.getTimerEndsAt().plusMinutes(extensionDuration));
            lane.setExtensionCount(lane.getExtensionCount() + 1);

            AuctionEvent extEvent = new AuctionEvent();
            extEvent.setAuction(auction);
            extEvent.setEventType(AuctionEventType.EXTENDED);
            extEvent.setMessage("Timer extended for lane: " + lane.getLane());
            extEvent.setActor("system");
            auctionEventRepository.save(extEvent);
        }

        // 10. Save lane
        auctionLaneRepository.save(lane);

        // 11. Return AuctionBidDto for the new bid
        return mapToDto(newBid);
    }

    private AuctionBidDto mapToDto(AuctionBid bid) {
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
}
