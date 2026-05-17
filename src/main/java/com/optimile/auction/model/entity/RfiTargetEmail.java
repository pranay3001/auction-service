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
@Table(name = "rfi_target_emails")
public class RfiTargetEmail {

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RfiTargetEmailId implements Serializable {
        @Column(name = "rfi_id")
        private UUID rfiId;

        @Column(name = "email")
        private String email;
    }

    @EmbeddedId
    private RfiTargetEmailId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("rfiId")
    @JoinColumn(name = "rfi_id")
    private Rfi rfi;
}
