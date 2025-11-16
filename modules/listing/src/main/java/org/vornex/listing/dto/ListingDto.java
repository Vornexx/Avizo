package org.vornex.listing.dto;

import org.vornex.listing.enums.ItemCondition;
import org.vornex.listing.enums.ListingStatus;
import org.vornex.listing.enums.ModerationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO для отображения Listing клиенту
 */
public record ListingDto(
        UUID id,
        String title,
        String description,
        BigDecimal priceAmount,
        String priceCurrency,
        String category,
        ItemCondition itemCondition,
        ListingStatus status,
        ModerationStatus moderationStatus,
        String moderationRejectionReason,
        UUID ownerId,
        String city,
        String region,
        Double latitude,
        Double longitude,
        Map<String,String> attributes,
        List<String> attachmentUrls,
        Long viewsCount,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt,
        Instant expiresAt,
        Long version
) {}
