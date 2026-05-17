package com.optimile.auction.repository;

import com.optimile.auction.model.entity.Contract;
import com.optimile.auction.model.enums.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface ContractRepository extends JpaRepository<Contract, UUID> {
    
    List<Contract> findByStatus(ContractStatus status);
    
    @Query("SELECT c FROM Contract c WHERE " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:vendorId IS NULL OR c.vendorId = :vendorId) AND " +
           "(:search IS NULL OR LOWER(c.lane) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Contract> findByFilters(@Param("status") ContractStatus status, 
                                 @Param("vendorId") UUID vendorId, 
                                 @Param("search") String search);

    List<Contract> findByStatusIn(List<ContractStatus> statuses);
    
    List<Contract> findByEndDateBeforeAndStatus(LocalDate date, ContractStatus status);
}
