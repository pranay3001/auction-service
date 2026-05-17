package com.optimile.auction.service;

import com.optimile.auction.model.entity.Contract;
import com.optimile.auction.model.enums.ContractStatus;
import com.optimile.auction.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContractExpiryScheduler {

    private final ContractRepository contractRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 1 * * *")
    public void updateContractStatuses() {
        LocalDate today = LocalDate.now();
        LocalDate expiryWindow = today.plusDays(30);

        log.info("Running contract expiry check for date: {}", today);

        // Mark EXPIRING_SOON: endDate <= today+30 AND status == ACTIVE
        List<Contract> active = contractRepository.findByStatus(ContractStatus.ACTIVE);
        for (Contract c : active) {
            if (c.getEndDate() != null && !c.getEndDate().isAfter(expiryWindow)) {
                c.setStatus(ContractStatus.EXPIRING_SOON);
                contractRepository.save(c);
                try {
                    notificationService.notifyContractExpiringSoon(c);
                } catch (Exception e) {
                    log.warn("Failed to send expiry notification for contract {}: {}", c.getId(), e.getMessage());
                }
            }
        }

        // Mark EXPIRED: endDate < today AND status == EXPIRING_SOON
        List<Contract> expiringSoon = contractRepository.findByStatus(ContractStatus.EXPIRING_SOON);
        for (Contract c : expiringSoon) {
            if (c.getEndDate() != null && c.getEndDate().isBefore(today)) {
                c.setStatus(ContractStatus.EXPIRED);
                contractRepository.save(c);
            }
        }

        log.info("Contract expiry check complete");
    }
}
