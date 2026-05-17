package com.optimile.auction.model.entity;

import com.optimile.auction.model.enums.RfiStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rfis")
public class Rfi {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private OffsetDateTime deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RfiStatus status = RfiStatus.DRAFT;

    @Column(name = "message_to_vendor", columnDefinition = "TEXT")
    private String messageToVendor;

    @Column(name = "template_file_name", length = 500)
    private String templateFileName;

    @Column(name = "created_by_id")
    private UUID createdById;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "rfi", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RfiTargetEmail> targetEmails = new ArrayList<>();

    @OneToMany(mappedBy = "rfi", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RfiVendorTracking> vendorTracking = new ArrayList<>();
}
