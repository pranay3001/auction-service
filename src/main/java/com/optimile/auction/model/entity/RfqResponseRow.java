package com.optimile.auction.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rfq_response_rows")
public class RfqResponseRow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "response_id", nullable = false)
    private RfqResponse response;

    @Column(nullable = false, length = 500)
    private String lane;

    @Column(name = "vehicle_type")
    private String vehicleType;

    @Column(nullable = false)
    private BigDecimal price;
}
