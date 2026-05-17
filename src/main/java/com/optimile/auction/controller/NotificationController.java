package com.optimile.auction.controller;

import com.optimile.auction.dto.NotificationDto;
import com.optimile.auction.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController extends BaseController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationDto>> getNotifications(
            HttpServletRequest request,
            @RequestParam Optional<Boolean> isRead,
            @RequestParam Optional<String> category) {
        UUID userId = extractUserId(request);
        if (userId == null) {
            userId = UUID.fromString("71234567-0000-0000-0000-000000000001");
        }
        return ResponseEntity.ok(notificationService.getNotifications(
                userId,
                isRead.orElse(null),
                category.orElse(null)
        ));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id) {
        notificationService.markRead(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllRead(HttpServletRequest request) {
        UUID userId = extractUserId(request);
        if (userId == null) {
            userId = UUID.fromString("71234567-0000-0000-0000-000000000001");
        }
        notificationService.markAllRead(userId);
        return ResponseEntity.noContent().build();
    }
}
