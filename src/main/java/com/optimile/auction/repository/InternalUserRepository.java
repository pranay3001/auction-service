package com.optimile.auction.repository;

import com.optimile.auction.model.entity.InternalUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InternalUserRepository extends JpaRepository<InternalUser, UUID> {
    Optional<InternalUser> findByEmail(String email);
}
