package com.optimile.auction.model.entity;

import com.optimile.auction.model.enums.AuctionStatus;
import com.optimile.auction.model.enums.AuctionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "auctions")
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 500)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuctionStatus status = AuctionStatus.DRAFT;

    @Column(name = "created_by_id")
    private UUID createdById;

    @Column(name = "created_by_role")
    private String createdByRole;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "start_at")
    private OffsetDateTime startAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "contract_start_date")
    private LocalDate contractStartDate;

    @Column(name = "contract_end_date")
    private LocalDate contractEndDate;

    @Column(name = "min_bid_decrement")
    private BigDecimal minBidDecrement = BigDecimal.ZERO;

    @Column(name = "extension_trigger_minutes")
    private Integer extensionTriggerMinutes = 5;

    @Column(name = "extension_duration_minutes")
    private Integer extensionDurationMinutes = 10;

    @Column(name = "max_extensions")
    private Integer maxExtensions = 3;

    @Column(name = "bidding_window_minutes")
    private Integer biddingWindowMinutes = 60;

    @Column(name = "booking_id")
    private UUID bookingId;

    private String region;

    @Column(name = "award_deadline")
    private OffsetDateTime awardDeadline;

    @OneToMany(mappedBy = "auction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AuctionLane> lanes = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "auction_invited_vendors",
        joinColumns = @JoinColumn(name = "auction_id"),
        inverseJoinColumns = @JoinColumn(name = "vendor_id")
    )
    private List<Vendor> invitedVendors = new ArrayList<>();
}
