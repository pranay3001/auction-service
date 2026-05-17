package com.optimile.auction.controller;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

public abstract class BaseController {

    protected UUID extractUserId(HttpServletRequest request) {
        String header = request.getHeader("X-User-Id");
        if (header == null || header.isBlank()) return null;
        try { return UUID.fromString(header); } catch (Exception e) { return null; }
    }

    protected String extractUserIdStr(HttpServletRequest request) {
        String header = request.getHeader("X-User-Id");
        return (header == null || header.isBlank()) ? "system" : header;
    }
}
