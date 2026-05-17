package com.optimile.auction.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class VendorServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${vendor.service.url:http://localhost:8083}")
    private String vendorServiceUrl;

    @Value("${vendor.service.key:optimile-internal-service-secret}")
    private String vendorServiceKey;

    public void pushNotification(List<UUID> vendorIds, String category,
                                  String title, String message, String deepLink) {
        if (vendorIds == null || vendorIds.isEmpty()) return;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Service-Key", vendorServiceKey);
            Map<String, Object> body = Map.of(
                "vendorIds", vendorIds,
                "category", category,
                "title", title,
                "message", message != null ? message : "",
                "deepLink", deepLink != null ? deepLink : ""
            );
            restTemplate.exchange(
                vendorServiceUrl + "/api/v1/internal/notifications",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Void.class
            );
        } catch (Exception e) {
            log.warn("Failed to push notification to vendor-service: {}", e.getMessage());
        }
    }
}
