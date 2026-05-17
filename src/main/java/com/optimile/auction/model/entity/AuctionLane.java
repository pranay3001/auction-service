package com.optimile.auction.model.entity;

import com.optimile.auction.model.enums.AllocationMode;
import com.optimile.auction.model.enums.RateUnit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "auction_lanes")
public class AuctionLane {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @Column(nullable = false, length = 500)
    private String lane;

    private String region;

    @Column(name = "vehicle_type")
    private String vehicleType;

    @Column(name = "capacity_mt")
    private BigDecimal capacityMt;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_unit", nullable = false)
    private RateUnit rateUnit = RateUnit.PER_TRIP;

    @Column(name = "ceiling_rate")
    private BigDecimal ceilingRate;

    @Column(name = "estimated_trips")
    private Integer estimatedTrips;

    @Column(name = "base_price_source")
    private String basePriceSource = "MANUAL";

    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_mode", nullable = false)
    private AllocationMode allocationMode = AllocationMode.SINGLE;

    @Column(name = "l1_allocation_pct")
    private BigDecimal l1AllocationPct;

    @Column(name = "l2_allocation_pct")
    private BigDecimal l2AllocationPct;

    @Column(name = "l3_allocation_pct")
    private BigDecimal l3AllocationPct;

    @Column(name = "timer_ends_at")
    private OffsetDateTime timerEndsAt;

    @Column(name = "extension_count", nullable = false)
    private Integer extensionCount = 0;

    @Column(name = "bid_count", nullable = false)
    private Integer bidCount = 0;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @ManyToMany
    @JoinTable(
        name = "auction_lane_eligible_vendors",
        joinColumns = @JoinColumn(name = "lane_id"),
        inverseJoinColumns = @JoinColumn(name = "vendor_id")
    )
    private List<Vendor> eligibleVendors = new ArrayList<>();
}
