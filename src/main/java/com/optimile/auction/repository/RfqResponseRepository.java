package com.optimile.auction.repository;

import com.optimile.auction.model.entity.RfqResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RfqResponseRepository extends JpaRepository<RfqResponse, UUID> {
    List<RfqResponse> findByRfqId(UUID rfqId);
}
