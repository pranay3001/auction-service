package com.optimile.auction.model.entity;

import com.optimile.auction.model.enums.AllocationRank;
import com.optimile.auction.model.enums.ContractStatus;
import com.optimile.auction.model.enums.ContractType;
import com.optimile.auction.model.enums.RateUnit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

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
@Table(name = "contracts")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "source_auction_id")
    private UUID sourceAuctionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "contract_type")
    private ContractType contractType;

    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;

    @Column(length = 255)
    private String vendorName;

    @Column(length = 500)
    private String lane;

    private String region;

    @Column(name = "vehicle_type")
    private String vehicleType;

    @Column(name = "contracted_rate", nullable = false)
    private BigDecimal contractedRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_unit", nullable = false)
    private RateUnit rateUnit = RateUnit.PER_TRIP;

    @Column(name = "volume_allocation_percent")
    private BigDecimal volumeAllocationPercent;

    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_rank")
    private AllocationRank allocationRank;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "estimated_trips")
    private Integer estimatedTrips;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractStatus status = ContractStatus.ACTIVE;

    @Column(name = "l1_override_reason", columnDefinition = "TEXT")
    private String l1OverrideReason;

    @Column(name = "rate_synced_to_tms", nullable = false)
    private Boolean rateSyncedToTms = false;

    @Column(name = "rate_deviation_open", nullable = false)
    private Boolean rateDeviationOpen = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlacementFailure> placementFailures = new ArrayList<>();
}
