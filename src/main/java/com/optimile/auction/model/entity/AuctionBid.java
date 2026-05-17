package com.optimile.auction.model.entity;

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
@Table(name = "auction_bids")
public class AuctionBid {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lane_id", nullable = false)
    private AuctionLane lane;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(name = "vendor_name")
    private String vendorName;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "bid_rank")
    private Integer bidRank;

    @Column(name = "is_current", nullable = false)
    private Boolean isCurrent = true;

    @CreationTimestamp
    @Column(name = "placed_at", nullable = false, updatable = false)
    private OffsetDateTime placedAt;
}
