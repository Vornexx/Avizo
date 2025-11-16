package org.vornex.listing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.vornex.listing.entity.Attachment;

import java.util.Optional;
import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    Optional<Attachment> findByStorageKey(String storageKey);

    @Query("select coalesce(max(a.position), 0) from Attachment a where a.listing.id = :listingId")
    int findMaxPositionByListingId(@Param("listingId") UUID listingId);

}
