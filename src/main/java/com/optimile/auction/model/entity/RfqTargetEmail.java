package com.optimile.auction.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rfq_target_emails")
public class RfqTargetEmail {

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RfqTargetEmailId implements Serializable {
        @Column(name = "rfq_id")
        private UUID rfqId;

        @Column(name = "email")
        private String email;
    }

    @EmbeddedId
    private RfqTargetEmailId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("rfqId")
    @JoinColumn(name = "rfq_id")
    private Rfq rfq;
}
