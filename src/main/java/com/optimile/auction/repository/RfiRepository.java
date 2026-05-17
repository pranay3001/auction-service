package com.optimile.auction.repository;

import com.optimile.auction.model.entity.Rfi;
import com.optimile.auction.model.enums.RfiStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RfiRepository extends JpaRepository<Rfi, UUID> {
    
    @Query("SELECT r FROM Rfi r WHERE " +
           "(:status IS NULL OR r.status = :status) AND " +
           "(:search IS NULL OR LOWER(r.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Rfi> findByFilters(@Param("status") RfiStatus status, 
                            @Param("search") String search);
}
