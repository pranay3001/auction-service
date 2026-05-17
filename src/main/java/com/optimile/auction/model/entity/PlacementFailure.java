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
@Table(name = "placement_failures")
public class PlacementFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(name = "failed_vendor", length = 255)
    private String failedVendor;

    @Column(name = "replacement_vendor", length = 255)
    private String replacementVendor;

    @Column(name = "original_rate")
    private BigDecimal originalRate;

    @Column(name = "replacement_rate")
    private BigDecimal replacementRate;

    private BigDecimal differential;

    @Column(name = "debit_note_triggered")
    private Boolean debitNoteTriggered = false;

    @CreationTimestamp
    @Column(name = "recorded_at", updatable = false)
    private OffsetDateTime recordedAt;
}
