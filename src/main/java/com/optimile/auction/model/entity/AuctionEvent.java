package com.optimile.auction.model.entity;

import com.optimile.auction.model.enums.AuctionEventType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "auction_events")
public class AuctionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private AuctionEventType eventType;

    private String message;

    private String actor;

    @CreationTimestamp
    @Column(name = "event_timestamp", nullable = false, updatable = false)
    private OffsetDateTime eventTimestamp;
}
