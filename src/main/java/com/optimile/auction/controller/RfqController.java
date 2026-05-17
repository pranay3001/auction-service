package com.optimile.auction.controller;

import com.optimile.auction.dto.CreateRfqRequest;
import com.optimile.auction.dto.PatchStatusRequest;
import com.optimile.auction.dto.RfqDto;
import com.optimile.auction.service.RfqService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rfqs")
@RequiredArgsConstructor
public class RfqController extends BaseController {

    private final RfqService rfqService;

    @GetMapping
    public ResponseEntity<List<RfqDto>> listRfqs(
            @RequestParam Optional<String> status,
            @RequestParam Optional<String> search) {
        return ResponseEntity.ok(rfqService.listRfqs(
                status.orElse(null),
                search.orElse(null)
        ));
    }

    @PostMapping
    public ResponseEntity<RfqDto> createRfq(
            @RequestBody @Valid CreateRfqRequest req,
            HttpServletRequest httpRequest) {
        UUID userId = extractUserId(httpRequest);
        return ResponseEntity.ok(rfqService.createRfq(req, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RfqDto> getRfq(@PathVariable UUID id) {
        return ResponseEntity.ok(rfqService.getRfq(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RfqDto> patchRfqStatus(
            @PathVariable UUID id,
            @RequestBody @Valid PatchStatusRequest req) {
        return ResponseEntity.ok(rfqService.patchStatus(id, req.getStatus()));
    }
}
