package com.optimile.auction.controller;

import com.optimile.auction.dto.CreateRfiRequest;
import com.optimile.auction.dto.PatchStatusRequest;
import com.optimile.auction.dto.RfiDto;
import com.optimile.auction.service.RfiService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/rfis")
@RequiredArgsConstructor
public class RfiController extends BaseController {

    private final RfiService rfiService;

    @GetMapping
    public ResponseEntity<List<RfiDto>> listRfis(
            @RequestParam Optional<String> status,
            @RequestParam Optional<String> search) {
        return ResponseEntity.ok(rfiService.listRfis(
                status.orElse(null),
                search.orElse(null)
        ));
    }

    @PostMapping
    public ResponseEntity<RfiDto> createRfi(
            @RequestBody @Valid CreateRfiRequest req,
            HttpServletRequest httpRequest) {
        UUID userId = extractUserId(httpRequest);
        return ResponseEntity.ok(rfiService.createRfi(req, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RfiDto> getRfi(@PathVariable UUID id) {
        return ResponseEntity.ok(rfiService.getRfi(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<RfiDto> patchRfiStatus(
            @PathVariable UUID id,
            @RequestBody @Valid PatchStatusRequest req) {
        return ResponseEntity.ok(rfiService.patchStatus(id, req.getStatus()));
    }
}
