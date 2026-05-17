package com.optimile.auction.controller;

import com.optimile.auction.dto.VendorDto;
import com.optimile.auction.model.entity.Vendor;
import com.optimile.auction.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorRepository vendorRepository;

    @GetMapping
    public ResponseEntity<?> listVendors() {
        List<Vendor> vendors = vendorRepository.findAll();
        List<VendorDto> result = vendors.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    private VendorDto mapToDto(Vendor v) {
        VendorDto dto = new VendorDto();
        dto.setId(v.getId());
        dto.setName(v.getName());
        dto.setEmail(v.getEmail());
        dto.setScore(v.getScore());
        dto.setCreatedAt(v.getCreatedAt());
        return dto;
    }
}
