package com.optimile.auction.service;

import com.optimile.auction.dto.ContractDto;
import com.optimile.auction.dto.PlacementFailureDto;
import com.optimile.auction.exception.ResourceNotFoundException;
import com.optimile.auction.model.entity.Contract;
import com.optimile.auction.model.entity.PlacementFailure;
import com.optimile.auction.model.enums.ContractStatus;
import com.optimile.auction.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;

    public List<ContractDto> listContracts(String status, String search, UUID vendorId) {
        List<Contract> contracts = contractRepository.findAll();

        return contracts.stream()
                .filter(c -> status == null || c.getStatus().name().equalsIgnoreCase(status))
                .filter(c -> search == null || (
                        (c.getVendorName() != null && c.getVendorName().toLowerCase().contains(search.toLowerCase()))
                        || (c.getLane() != null && c.getLane().toLowerCase().contains(search.toLowerCase()))
                ))
                .filter(c -> vendorId == null || vendorId.equals(c.getVendorId()))
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public ContractDto getContract(UUID id) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + id));
        return mapToDto(contract);
    }

    public ContractDto terminateContract(UUID id) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + id));
        contract.setStatus(ContractStatus.TERMINATED);
        Contract saved = contractRepository.save(contract);
        return mapToDto(saved);
    }

    // --- Private helpers ---

    public ContractDto mapToDto(Contract c) {
        List<PlacementFailureDto> failures = c.getPlacementFailures().stream()
                .map(this::mapFailureToDto)
                .collect(Collectors.toList());

        return ContractDto.builder()
                .id(c.getId())
                .sourceAuctionId(c.getSourceAuctionId())
                .contractType(c.getContractType() != null ? c.getContractType().name() : null)
                .vendorId(c.getVendorId())
                .vendorName(c.getVendorName())
                .lane(c.getLane())
                .region(c.getRegion())
                .vehicleType(c.getVehicleType())
                .contractedRate(c.getContractedRate())
                .rateUnit(c.getRateUnit() != null ? c.getRateUnit().name() : null)
                .volumeAllocationPercent(c.getVolumeAllocationPercent())
                .allocationRank(c.getAllocationRank() != null ? c.getAllocationRank().name() : null)
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .estimatedTrips(c.getEstimatedTrips())
                .status(c.getStatus() != null ? c.getStatus().name() : null)
                .l1OverrideReason(c.getL1OverrideReason())
                .rateSyncedToTms(c.getRateSyncedToTms())
                .rateDeviationOpen(c.getRateDeviationOpen())
                .createdAt(c.getCreatedAt())
                .placementFailures(failures)
                .build();
    }

    private PlacementFailureDto mapFailureToDto(PlacementFailure pf) {
        return PlacementFailureDto.builder()
                .id(pf.getId())
                .failedVendor(pf.getFailedVendor())
                .replacementVendor(pf.getReplacementVendor())
                .originalRate(pf.getOriginalRate())
                .replacementRate(pf.getReplacementRate())
                .differential(pf.getDifferential())
                .debitNoteTriggered(pf.getDebitNoteTriggered())
                .recordedAt(pf.getRecordedAt())
                .build();
    }
}
