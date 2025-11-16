package org.vornex.listing.dto;

import org.vornex.listing.Money;
import org.vornex.listing.dto.attachmentDto.AttachmentDto;
import org.vornex.listing.enums.ItemCondition;
import org.vornex.listing.enums.ListingStatus;
import org.vornex.listing.enums.ModerationStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// DTO для ответа
public record ListingResponseDto(
        UUID id,
        String title,
        String description,
        Money price,
        String category,
        ItemCondition itemCondition,
        ListingStatus status,
        ModerationStatus moderationStatus,
        UUID ownerId,
        Instant createdAt,
        Instant updatedAt,
        Instant publishedAt,
        long viewsCount,
        List<AttachmentDto> attachments
) {}