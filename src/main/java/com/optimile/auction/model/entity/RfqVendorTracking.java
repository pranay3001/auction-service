package com.optimile.auction.model.entity;

import com.optimile.auction.model.enums.VendorRespStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rfq_vendor_tracking")
public class RfqVendorTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfq_id")
    private Rfq rfq;

    @Column(name = "vendor_id_or_email", length = 255)
    private String vendorIdOrEmail;

    @Column(length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VendorRespStatus status = VendorRespStatus.PENDING;
}
