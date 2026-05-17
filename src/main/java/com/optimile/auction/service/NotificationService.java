package com.optimile.auction.service;

import com.optimile.auction.client.VendorServiceClient;
import com.optimile.auction.dto.NotificationDto;
import com.optimile.auction.exception.ResourceNotFoundException;
import com.optimile.auction.model.entity.Auction;
import com.optimile.auction.model.entity.Contract;
import com.optimile.auction.model.entity.Notification;
import com.optimile.auction.model.entity.Rfi;
import com.optimile.auction.model.entity.Rfq;
import com.optimile.auction.model.enums.NotifCategory;
import com.optimile.auction.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationService {

    private static final UUID DEV_USER_ID = UUID.fromString("71234567-0000-0000-0000-000000000001");

    private final NotificationRepository notificationRepository;
    private final VendorServiceClient vendorServiceClient;

    public void notifyAuctionLive(Auction auction) {
        createNotification(
                DEV_USER_ID,
                NotifCategory.AUCTIONS,
                "Auction launched",
                auction.getTitle() + " is now live",
                "/auction/auctions/" + auction.getId()
        );
        vendorServiceClient.pushNotification(
                auction.getInvitedVendors().stream().map(v -> v.getId()).collect(java.util.stream.Collectors.toList()),
                "AUCTIONS",
                "Auction " + auction.getTitle() + " is now LIVE",
                "Bidding is now open. Submit your bids before the timer ends.",
                "/vendor/sourcing/auctions/" + auction.getId()
        );
    }

    public void notifyAuctionCompleted(Auction auction) {
        createNotification(
                DEV_USER_ID,
                NotifCategory.AWARDS,
                "Auction completed",
                auction.getTitle() + " has ended",
                "/auction/auctions/" + auction.getId()
        );
    }

    public void notifyAuctionAwarded(Auction auction) {
        createNotification(
                DEV_USER_ID,
                NotifCategory.AWARDS,
                "Auction awarded",
                auction.getTitle() + " has been awarded",
                "/auction/auctions/" + auction.getId()
        );
        vendorServiceClient.pushNotification(
                auction.getInvitedVendors().stream().map(v -> v.getId()).collect(java.util.stream.Collectors.toList()),
                "AUCTIONS",
                "Auction awarded - contract created",
                auction.getTitle() + " has been awarded",
                "/vendor/sourcing/auctions/" + auction.getId()
        );
    }

    public void notifyContractExpiringSoon(Contract contract) {
        createNotification(
                DEV_USER_ID,
                NotifCategory.CONTRACTS,
                "Contract expiring soon",
                "Contract with " + contract.getVendorName() + " expires on " + contract.getEndDate(),
                "/auction/contracts/" + contract.getId()
        );
    }

    public void notifyRfiPublished(Rfi rfi) {
        createNotification(
                DEV_USER_ID,
                NotifCategory.SOURCING,
                "New RFI published",
                rfi.getTitle() + " has been published",
                null
        );
    }

    public void notifyRfqPublished(Rfq rfq) {
        createNotification(
                DEV_USER_ID,
                NotifCategory.SOURCING,
                "New RFQ published",
                rfq.getTitle() + " has been published",
                null
        );
    }

    public List<NotificationDto> getNotifications(UUID userId, Boolean isRead, String category) {
        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return notifications.stream()
                .filter(n -> isRead == null || n.getIsRead().equals(isRead))
                .filter(n -> category == null || n.getCategory().name().equalsIgnoreCase(category))
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public void markRead(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    public void markAllRead(UUID userId) {
        notificationRepository.markAllReadForUser(userId);
    }

    // --- Private helpers ---

    private void createNotification(UUID userId, NotifCategory category, String title, String message, String deepLink) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setCategory(category);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setIsRead(false);
        notification.setDeepLink(deepLink);
        notificationRepository.save(notification);
    }

    private NotificationDto mapToDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .category(n.getCategory().name())
                .title(n.getTitle())
                .message(n.getMessage())
                .isRead(n.getIsRead())
                .deepLink(n.getDeepLink())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
