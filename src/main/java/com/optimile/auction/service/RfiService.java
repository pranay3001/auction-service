package com.optimile.auction.service;

import com.optimile.auction.dto.CreateRfiRequest;
import com.optimile.auction.dto.RfiDto;
import com.optimile.auction.dto.SourcingVendorTrackingDto;
import com.optimile.auction.exception.ResourceNotFoundException;
import com.optimile.auction.model.entity.Rfi;
import com.optimile.auction.model.entity.RfiTargetEmail;
import com.optimile.auction.model.entity.RfiVendorTracking;
import com.optimile.auction.model.enums.RfiStatus;
import com.optimile.auction.model.enums.VendorRespStatus;
import com.optimile.auction.repository.RfiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class RfiService {

    private final RfiRepository rfiRepository;
    private final NotificationService notificationService;

    public List<RfiDto> listRfis(String status, String search) {
        List<Rfi> rfis = rfiRepository.findAll();

        return rfis.stream()
                .filter(r -> status == null || r.getStatus().name().equalsIgnoreCase(status))
                .filter(r -> search == null || (r.getTitle() != null && r.getTitle().toLowerCase().contains(search.toLowerCase())))
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public RfiDto createRfi(CreateRfiRequest req, UUID userId) {
        Rfi rfi = new Rfi();
        rfi.setTitle(req.getTitle());
        rfi.setDescription(req.getDescription());
        rfi.setDeadline(req.getDeadline());
        rfi.setStatus(RfiStatus.PUBLISHED);
        rfi.setMessageToVendor(req.getMessageToVendor());
        rfi.setTemplateFileName(req.getTemplateFileName());
        rfi.setCreatedById(userId);

        Rfi savedRfi = rfiRepository.save(rfi);

        if (req.getTargetEmails() != null) {
            for (String email : req.getTargetEmails()) {
                // Create RfiTargetEmail
                RfiTargetEmail targetEmail = new RfiTargetEmail();
                targetEmail.setId(new RfiTargetEmail.RfiTargetEmailId(savedRfi.getId(), email));
                targetEmail.setRfi(savedRfi);
                savedRfi.getTargetEmails().add(targetEmail);

                // Create RfiVendorTracking
                RfiVendorTracking tracking = new RfiVendorTracking();
                tracking.setRfi(savedRfi);
                tracking.setVendorIdOrEmail(email);
                tracking.setStatus(VendorRespStatus.PENDING);
                savedRfi.getVendorTracking().add(tracking);
            }
        }

        savedRfi = rfiRepository.save(savedRfi);

        try {
            notificationService.notifyRfiPublished(savedRfi);
        } catch (Exception e) {
            // Notification failure should not fail the request
        }

        return mapToDto(savedRfi);
    }

    public RfiDto getRfi(UUID id) {
        Rfi rfi = rfiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RFI not found: " + id));
        return mapToDto(rfi);
    }

    public RfiDto patchStatus(UUID id, String status) {
        Rfi rfi = rfiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RFI not found: " + id));
        rfi.setStatus(RfiStatus.valueOf(status));
        Rfi saved = rfiRepository.save(rfi);
        return mapToDto(saved);
    }

    // --- Private helpers ---

    private RfiDto mapToDto(Rfi rfi) {
        List<String> emails = rfi.getTargetEmails().stream()
                .map(e -> e.getId().getEmail())
                .collect(Collectors.toList());

        List<SourcingVendorTrackingDto> tracking = rfi.getVendorTracking().stream()
                .map(t -> SourcingVendorTrackingDto.builder()
                        .vendorIdOrEmail(t.getVendorIdOrEmail())
                        .name(t.getName())
                        .status(t.getStatus().name())
                        .build())
                .collect(Collectors.toList());

        return RfiDto.builder()
                .id(rfi.getId())
                .title(rfi.getTitle())
                .description(rfi.getDescription())
                .deadline(rfi.getDeadline())
                .status(rfi.getStatus().name())
                .messageToVendor(rfi.getMessageToVendor())
                .templateFileName(rfi.getTemplateFileName())
                .createdById(rfi.getCreatedById())
                .createdAt(rfi.getCreatedAt())
                .updatedAt(rfi.getUpdatedAt())
                .targetEmails(emails)
                .vendorTracking(tracking)
                .build();
    }
}
