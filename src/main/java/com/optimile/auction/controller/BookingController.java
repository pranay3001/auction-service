package com.optimile.auction.controller;

import com.optimile.auction.dto.BookingDto;
import com.optimile.auction.exception.ResourceNotFoundException;
import com.optimile.auction.model.entity.Booking;
import com.optimile.auction.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingRepository bookingRepository;

    @GetMapping
    public ResponseEntity<?> listBookings(@RequestParam Optional<String> status) {
        List<Booking> bookings = bookingRepository.findAll();
        List<BookingDto> result = bookings.stream()
                .filter(b -> status.isEmpty()
                        || b.getStatus().name().equalsIgnoreCase(status.get()))
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBooking(@PathVariable UUID id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + id));
        return ResponseEntity.ok(mapToDto(booking));
    }

    private BookingDto mapToDto(Booking b) {
        BookingDto dto = new BookingDto();
        dto.setId(b.getId());
        dto.setLane(b.getLane());
        dto.setVehicleType(b.getVehicleType());
        dto.setCommodity(b.getCommodity());
        dto.setQuantity(b.getQuantity());
        dto.setUom(b.getUom());
        dto.setLoadingDate(b.getLoadingDate());
        dto.setStatus(b.getStatus() != null ? b.getStatus().name() : null);
        dto.setCreatedAt(b.getCreatedAt());
        return dto;
    }
}
