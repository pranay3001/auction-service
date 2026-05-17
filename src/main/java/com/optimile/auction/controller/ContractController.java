package com.optimile.auction.controller;

import com.optimile.auction.dto.ContractDto;
import com.optimile.auction.service.ContractService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @GetMapping
    public ResponseEntity<List<ContractDto>> listContracts(
            @RequestParam Optional<String> status,
            @RequestParam Optional<String> search,
            @RequestParam Optional<UUID> vendorId) {
        return ResponseEntity.ok(contractService.listContracts(
                status.orElse(null),
                search.orElse(null),
                vendorId.orElse(null)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ContractDto> getContract(@PathVariable UUID id) {
        return ResponseEntity.ok(contractService.getContract(id));
    }

    @PostMapping("/{id}/terminate")
    public ResponseEntity<ContractDto> terminateContract(@PathVariable UUID id) {
        return ResponseEntity.ok(contractService.terminateContract(id));
    }
}
