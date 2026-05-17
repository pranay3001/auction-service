package com.optimile.auction.repository;

import com.optimile.auction.model.entity.AuctionEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuctionEventRepository extends JpaRepository<AuctionEvent, UUID> {
    List<AuctionEvent> findByAuctionIdOrderByEventTimestampDesc(UUID auctionId);
    List<AuctionEvent> findByAuctionIdOrderByEventTimestampAsc(UUID auctionId);
}
