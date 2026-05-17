package com.optimile.auction.controller;

import com.optimile.auction.dto.*;
import com.optimile.auction.service.AuctionService;
import com.optimile.auction.service.AwardService;
import com.optimile.auction.service.BidService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
public class AuctionController extends BaseController {

    private final AuctionService auctionService;
    private final BidService bidService;
    private final AwardService awardService;

    @GetMapping
    public ResponseEntity<?> listAuctions(
            @RequestParam Optional<String> status,
            @RequestParam Optional<String> type,
            @RequestParam Optional<String> search,
            @RequestParam(required = false) UUID invitedVendorId) {
        List<AuctionDto> auctions = auctionService.listAuctions(
                status.orElse(null),
                type.orElse(null),
                search.orElse(null),
                invitedVendorId);
        return ResponseEntity.ok(auctions);
    }

    @PostMapping
    public ResponseEntity<?> createAuction(
            @RequestBody @Valid CreateAuctionRequest req,
            HttpServletRequest request) {
        UUID userId = extractUserId(request);
        AuctionDto dto = auctionService.createAuction(req, userId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAuction(@PathVariable UUID id) {
        AuctionDto dto = auctionService.getAuction(id);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/launch")
    public ResponseEntity<?> launchAuction(
            @PathVariable UUID id,
            HttpServletRequest request) {
        UUID userId = extractUserId(request);
        AuctionDto dto = auctionService.launchAuction(id, userId);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelAuction(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body,
            HttpServletRequest request) {
        UUID userId = extractUserId(request);
        String reason = body != null ? body.get("reason") : null;
        AuctionDto dto = auctionService.cancelAuction(id, userId, reason);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeAuction(@PathVariable UUID id) {
        AuctionDto dto = auctionService.completeAuction(id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{auctionId}/lanes/{laneId}/bids")
    public ResponseEntity<?> getBids(
            @PathVariable UUID auctionId,
            @PathVariable UUID laneId) {
        List<AuctionBidDto> bids = bidService.getBids(auctionId, laneId);
        return ResponseEntity.ok(bids);
    }

    @PostMapping("/{auctionId}/lanes/{laneId}/bids")
    public ResponseEntity<?> placeBid(
            @PathVariable UUID auctionId,
            @PathVariable UUID laneId,
            @RequestBody @Valid PlaceBidRequest req,
            HttpServletRequest request) {
        AuctionBidDto dto = bidService.placeBid(auctionId, laneId, req);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{auctionId}/award")
    public ResponseEntity<?> awardAuction(
            @PathVariable UUID auctionId,
            @RequestBody List<AwardDecisionRequest> requests) {
        List<AwardDecisionDto> decisions = awardService.awardAuction(auctionId, requests);
        return ResponseEntity.ok(decisions);
    }

    @PostMapping("/{auctionId}/finalize")
    public ResponseEntity<?> finalizeAuction(@PathVariable UUID auctionId) {
        List<ContractDto> contracts = awardService.finalizeAuction(auctionId);
        return ResponseEntity.ok(contracts);
    }

    @PostMapping("/{auctionId}/reject")
    public ResponseEntity<?> rejectAuction(
            @PathVariable UUID auctionId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.get("reason") : null;
        AuctionDto dto = awardService.rejectAuction(auctionId, reason);
        return ResponseEntity.ok(dto);
    }
}
