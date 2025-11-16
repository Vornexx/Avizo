package org.vornex.listing.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.vornex.listing.entity.Listing;

import java.util.Optional;
import java.util.UUID;

public interface ListingRepository extends JpaRepository<Listing, UUID> {

    // Atomic increment of views without loading entity (better under high concurrency).
    @Modifying
    @Query("update Listing l set l.viewsCount = l.viewsCount + 1 where l.id = :id")
    int incrementViews(@Param("id") UUID id);

    // Метод чтобы получить listing с attachments (fetch join) при необходимости
    @Query("select l from Listing l left join fetch l.attachments a where l.id = :id")
    Optional<Listing> findByIdWithAttachments(@Param("id") UUID id); 
}
