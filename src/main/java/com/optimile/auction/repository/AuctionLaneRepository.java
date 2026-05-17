package com.optimile.auction.repository;

import com.optimile.auction.model.entity.AuctionLane;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AuctionLaneRepository extends JpaRepository<AuctionLane, UUID> {
}
