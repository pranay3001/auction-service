package com.optimile.auction.service;

import com.optimile.auction.dto.*;
import com.optimile.auction.exception.ResourceNotFoundException;
import com.optimile.auction.model.entity.*;
import com.optimile.auction.model.enums.RfqStatus;
import com.optimile.auction.model.enums.VendorRespStatus;
import com.optimile.auction.repository.RfqRepository;
import com.optimile.auction.repository.RfqResponseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class RfqService {

    private final RfqRepository rfqRepository;
    private final RfqResponseRepository rfqResponseRepository;
    private final NotificationService notificationService;

    public List<RfqDto> listRfqs(String status, String search) {
        List<Rfq> rfqs = rfqRepository.findAll();

        return rfqs.stream()
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

    public RfqDto createRfq(CreateRfqRequest req, UUID userId) {
        Rfq rfq = new Rfq();
        rfq.setTitle(req.getTitle());
        rfq.setDeadline(req.getDeadline());
        rfq.setStatus(RfqStatus.PUBLISHED);
        rfq.setMessageToVendor(req.getMessageToVendor());
        rfq.setTemplateFileName(req.getTemplateFileName());
        rfq.setCreatedById(userId);

        Rfq savedRfq = rfqRepository.save(rfq);

        if (req.getTargetEmails() != null) {
            for (String email : req.getTargetEmails()) {
                // Create RfqTargetEmail
                RfqTargetEmail targetEmail = new RfqTargetEmail();
                targetEmail.setId(new RfqTargetEmail.RfqTargetEmailId(savedRfq.getId(), email));
                targetEmail.setRfq(savedRfq);
                savedRfq.getTargetEmails().add(targetEmail);

                // Create RfqVendorTracking
                RfqVendorTracking tracking = new RfqVendorTracking();
                tracking.setRfq(savedRfq);
                tracking.setVendorIdOrEmail(email);
                tracking.setStatus(VendorRespStatus.PENDING);
                savedRfq.getVendorTracking().add(tracking);
            }
        }

        savedRfq = rfqRepository.save(savedRfq);

        try {
            notificationService.notifyRfqPublished(savedRfq);
        } catch (Exception e) {
            // Notification failure should not fail the request
        }

        return mapToDto(savedRfq);
    }

    public RfqDto getRfq(UUID id) {
        Rfq rfq = rfqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RFQ not found: " + id));
        return mapToDto(rfq);
    }

    public RfqDto patchStatus(UUID id, String status) {
        Rfq rfq = rfqRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RFQ not found: " + id));
        rfq.setStatus(RfqStatus.valueOf(status));
        Rfq saved = rfqRepository.save(rfq);
        return mapToDto(saved);
    }

    public RfqResponseDto uploadResponse(UUID rfqId, UploadRfqResponseRequest req) {
        Rfq rfq = rfqRepository.findById(rfqId)
                .orElseThrow(() -> new ResourceNotFoundException("RFQ not found: " + rfqId));

        RfqResponse response = new RfqResponse();
        response.setRfq(rfq);
        response.setVendorName(req.getVendorName());
        response.setFileName(req.getFileName());

        if (req.getRows() != null) {
            for (RfqResponseRowDto rowDto : req.getRows()) {
                RfqResponseRow row = new RfqResponseRow();
                row.setResponse(response);
                row.setLane(rowDto.getLane());
                row.setVehicleType(rowDto.getVehicleType());
                row.setPrice(rowDto.getPrice());
                response.getRows().add(row);
            }
        }

        rfq.getResponses().add(response);

        // Mark matching vendor tracking as RESPONDED
        if (req.getVendorName() != null) {
            for (RfqVendorTracking tracking : rfq.getVendorTracking()) {
                if (req.getVendorName().equalsIgnoreCase(tracking.getVendorIdOrEmail())
                        || req.getVendorName().equalsIgnoreCase(tracking.getName())) {
                    tracking.setStatus(VendorRespStatus.RESPONDED);
                }
            }
        }

        rfqRepository.save(rfq);

        return mapResponseToDto(response);
    }

    public List<RfqResponseDto> getResponsesForRfq(UUID rfqId) {
        List<RfqResponse> responses = rfqResponseRepository.findByRfqId(rfqId);
        return responses.stream()
                .map(this::mapResponseToDto)
                .collect(Collectors.toList());
    }

    public List<RfqResponseDto> getAllResponses(String search, LocalDate from, LocalDate to) {
        List<RfqResponse> responses = rfqResponseRepository.findAll();

        return responses.stream()
                .filter(r -> search == null || (
                        (r.getVendorName() != null && r.getVendorName().toLowerCase().contains(search.toLowerCase()))
                        || r.getRows().stream().anyMatch(row -> row.getLane() != null
                                && row.getLane().toLowerCase().contains(search.toLowerCase()))
                ))
                .filter(r -> from == null || (r.getUploadedAt() != null
                        && !r.getUploadedAt().toLocalDate().isBefore(from)))
                .filter(r -> to == null || (r.getUploadedAt() != null
                        && !r.getUploadedAt().toLocalDate().isAfter(to)))
                .map(this::mapResponseToDto)
                .collect(Collectors.toList());
    }

    // --- Private helpers ---

    private RfqDto mapToDto(Rfq rfq) {
        List<String> emails = rfq.getTargetEmails().stream()
                .map(e -> e.getId().getEmail())
                .collect(Collectors.toList());

        List<SourcingVendorTrackingDto> tracking = rfq.getVendorTracking().stream()
                .map(t -> SourcingVendorTrackingDto.builder()
                        .vendorIdOrEmail(t.getVendorIdOrEmail())
                        .name(t.getName())
                        .status(t.getStatus().name())
                        .build())
                .collect(Collectors.toList());

        return RfqDto.builder()
                .id(rfq.getId())
                .title(rfq.getTitle())
                .deadline(rfq.getDeadline())
                .status(rfq.getStatus().name())
                .messageToVendor(rfq.getMessageToVendor())
                .templateFileName(rfq.getTemplateFileName())
                .createdById(rfq.getCreatedById())
                .createdAt(rfq.getCreatedAt())
                .updatedAt(rfq.getUpdatedAt())
                .targetEmails(emails)
                .vendorTracking(tracking)
                .build();
    }

    public RfqResponseDto mapResponseToDto(RfqResponse r) {
        List<RfqResponseRowDto> rows = r.getRows().stream()
                .map(row -> RfqResponseRowDto.builder()
                        .lane(row.getLane())
                        .vehicleType(row.getVehicleType())
                        .price(row.getPrice())
                        .build())
                .collect(Collectors.toList());

        UUID rfqId = r.getRfq() != null ? r.getRfq().getId() : null;

        return RfqResponseDto.builder()
                .id(r.getId())
                .rfqId(rfqId)
                .vendorName(r.getVendorName())
                .fileName(r.getFileName())
                .uploadedBy(r.getUploadedBy())
                .uploadedAt(r.getUploadedAt())
                .rows(rows)
                .build();
    }
}
