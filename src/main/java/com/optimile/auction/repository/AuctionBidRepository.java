package com.optimile.auction.repository;

import com.optimile.auction.model.entity.AuctionBid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuctionBidRepository extends JpaRepository<AuctionBid, UUID> {
    List<AuctionBid> findByLaneIdAndIsCurrentTrue(UUID laneId);
    List<AuctionBid> findByLaneIdAndIsCurrentTrueOrderByAmountAsc(UUID laneId);
    List<AuctionBid> findByLaneIdOrderByAmountAsc(UUID laneId);
    List<AuctionBid> findByAuctionId(UUID auctionId);
}
