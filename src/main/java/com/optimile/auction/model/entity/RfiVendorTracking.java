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
@Table(name = "rfi_vendor_tracking")
public class RfiVendorTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rfi_id", nullable = false)
    private Rfi rfi;

    @Column(name = "vendor_id_or_email", length = 255, nullable = false)
    private String vendorIdOrEmail;

    @Column(length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VendorRespStatus status = VendorRespStatus.PENDING;
}
