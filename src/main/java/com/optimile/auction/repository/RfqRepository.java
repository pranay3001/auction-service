package com.optimile.auction.repository;

import com.optimile.auction.model.entity.Rfq;
import com.optimile.auction.model.enums.RfqStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RfqRepository extends JpaRepository<Rfq, UUID> {
    
    @Query("SELECT r FROM Rfq r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:search IS NULL OR LOWER(r.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Rfq> findByFilters(@Param("status") RfqStatus status, 
                            @Param("search") String search);
}
