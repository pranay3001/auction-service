package com.optimile.auction.controller;

import com.optimile.auction.dto.RfqResponseDto;
import com.optimile.auction.dto.UploadRfqResponseRequest;
import com.optimile.auction.service.RfqService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RfqResponseController {

    private final RfqService rfqService;

    @GetMapping("/rfq-responses")
    public ResponseEntity<List<RfqResponseDto>> getAllResponses(
            @RequestParam Optional<String> search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(rfqService.getAllResponses(
                search.orElse(null),
                from,
                to
        ));
    }

    @PostMapping("/rfqs/{rfqId}/responses")
    public ResponseEntity<RfqResponseDto> uploadResponse(
            @PathVariable UUID rfqId,
            @RequestBody @Valid UploadRfqResponseRequest req) {
        return ResponseEntity.ok(rfqService.uploadResponse(rfqId, req));
    }

    @GetMapping("/rfqs/{rfqId}/responses")
    public ResponseEntity<List<RfqResponseDto>> getResponsesForRfq(@PathVariable UUID rfqId) {
        return ResponseEntity.ok(rfqService.getResponsesForRfq(rfqId));
    }
}
