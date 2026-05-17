package com.optimile.auction.repository;

import com.optimile.auction.model.entity.Auction;
import com.optimile.auction.model.enums.AuctionStatus;
import com.optimile.auction.model.enums.AuctionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, UUID> {
    
    List<Auction> findByStatus(AuctionStatus status);
    
    List<Auction> findByType(AuctionType type);
    
    @Query("SELECT a FROM Auction a WHERE " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:type IS NULL OR a.type = :type) AND " +
           "(:search IS NULL OR LOWER(a.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Auction> findByFilters(@Param("status") AuctionStatus status, 
                                @Param("type") AuctionType type, 
                                @Param("search") String search);

    List<Auction> findTop4ByOrderByUpdatedAtDesc();
    List<Auction> findTop4ByStatusInOrderByUpdatedAtDesc(List<AuctionStatus> statuses);

    @Query("SELECT DISTINCT a FROM Auction a JOIN a.invitedVendors v WHERE v.id = :vendorId")
    List<Auction> findByInvitedVendorId(@Param("vendorId") UUID vendorId);

    @Query("SELECT DISTINCT a FROM Auction a JOIN a.invitedVendors v WHERE v.id = :vendorId AND a.status = :status")
    List<Auction> findByInvitedVendorIdAndStatus(@Param("vendorId") UUID vendorId, @Param("status") AuctionStatus status);
}
