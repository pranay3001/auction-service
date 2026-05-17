package com.optimile.auction.repository;

import com.optimile.auction.model.entity.AwardDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AwardDecisionRepository extends JpaRepository<AwardDecision, UUID> {
    List<AwardDecision> findByAuctionId(UUID auctionId);
    List<AwardDecision> findByLaneId(UUID laneId);
    void deleteByAuctionId(UUID auctionId);
}
