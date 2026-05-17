package com.optimile.auction.model.entity;

import com.optimile.auction.model.enums.AllocationRank;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "award_decisions")
public class AwardDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lane_id", nullable = false)
    private AuctionLane lane;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "vendor_name")
    private String vendorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_rank", nullable = false)
    private AllocationRank allocationRank;

    @Enumerated(EnumType.STRING)
    @Column(name = "awarded_bid_rank")
    private AllocationRank awardedBidRank;

    @Column(name = "awarded_amount", nullable = false)
    private BigDecimal awardedAmount;

    @Column(name = "override_reason")
    private String overrideReason;

    @Column(name = "allocation_percent")
    private BigDecimal allocationPercent;

    @CreationTimestamp
    @Column(name = "decided_at", nullable = false, updatable = false)
    private OffsetDateTime decidedAt;
}
